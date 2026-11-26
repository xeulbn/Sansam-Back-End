package org.example.sansam.global.lab;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class RequestTrace extends OncePerRequestFilter {
    static final ThreadLocal<RequestTrace.Sample> ACTIVE = new ThreadLocal<>();
    static class Lease {
        long waitNanos, heldNanos;
        boolean stock;
    }

    static class Sample {
        int reads, writes;
        final List<Lease> leases = new ArrayList<>();
    }

    private final PrintWriter writer;

    public RequestTrace() throws IOException {
        writer = new PrintWriter(Files.newBufferedWriter(Path.of(System.getenv("LAB_TRACE"))));
        writer.println("phase,id,start_ms,end_ms,status,reads,writes,leases_wait_hold_stock_ns");
    }

    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse resp, FilterChain chain)
            throws ServletException, IOException {
        // 주문 생성 API만 측정 대상으로 본다.
        String orderPath = req.getContextPath() + "/api/orders/save";
        if (!"POST".equals(req.getMethod()) || !orderPath.equals(req.getRequestURI())) {
            chain.doFilter(req, resp);
            return;
        }

        String id = req.getHeader("X-Lab-Id");
        String phase = req.getHeader("X-Lab-Phase");

        // 일반 주문: 측정 기록을 만들지 않고 기존 주문 처리로 넘긴다.
        if (id == null && phase == null) {
            chain.doFilter(req, resp);
            return;
        }

        // 실험 헤더를 보냈다면 두 값이 모두 유효해야 한다.
        if (id == null || !id.matches("[0-9]+")
                || phase == null || !phase.matches("[a-zA-Z0-9_-]+")) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid lab headers");
            return;
        }

        // 이 아래부터 기존 코드: Sample 생성 → ACTIVE.set(s) → 주문 처리 → CSV 기록
        Sample s = new Sample();
        long start = System.currentTimeMillis();
        ACTIVE.set(s);
        boolean completed = false;
        try {
            chain.doFilter(req, resp);
            completed = true;
        } finally {
            ACTIVE.remove();
            String leases = s.leases.stream().map(x -> x.waitNanos + ":" + x.heldNanos + ":" + (x.stock ? 1 : 0))
                    .collect(Collectors.joining("|"));
            synchronized (writer) {
                writer.println(phase + "," + id + "," + start + "," + System.currentTimeMillis() + ","
                        + (completed ? resp.getStatus() : 500) + "," + s.reads + "," + s.writes + "," + leases);
                writer.flush();
            }
        }
    }
}
