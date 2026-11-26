package org.example.sansam.global.lab;

import org.springframework.jdbc.datasource.DelegatingDataSource;

import javax.sql.DataSource;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Locale;
import java.util.regex.Pattern;

public class MeasuredPool extends DelegatingDataSource {

    private static final Pattern READ = Pattern.compile("\\bfrom\\s+`?stock`?\\b");
    private static final Pattern WRITE = Pattern.compile("^update\\s+`?stock`?\\s");

    public MeasuredPool(DataSource realPool) {
        super(realPool);
    }

    @Override
    public Connection getConnection() throws SQLException {
        return acquire(null, null);
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return acquire(username, password);
    }

    private Connection acquire(String username, String password) throws SQLException {
        RequestTrace.Sample sample = RequestTrace.ACTIVE.get();

        // 1. 풀에 커넥션을 요청하기 직전부터 실제로 빌릴 때까지
        long start = System.nanoTime();
        Connection connection = username == null
                ? super.getConnection()
                : super.getConnection(username, password);
        long acquired = System.nanoTime();

        if (sample == null) {
            return connection;
        }

        RequestTrace.Lease lease = new RequestTrace.Lease();
        lease.waitNanos = acquired - start;
        sample.leases.add(lease);

        return proxy(Connection.class, (ignored, method, args) -> {
            if (method.getName().equals("close")) {
                try {
                    connection.close();
                } finally {
                    // 2. 실제로 빌린 시각부터 풀에 반환하는 호출이 끝날 때까지
                    if (lease.heldNanos == 0) {
                        lease.heldNanos = System.nanoTime() - acquired;
                    }
                }
                return null;
            }

            Object result = invoke(connection, method, args);

            if (method.getName().equals("prepareStatement") && args[0] instanceof String sql) {
                String query = sql.stripLeading().toLowerCase(Locale.ROOT);
                boolean read = READ.matcher(query).find();
                boolean write = WRITE.matcher(query).find();

                if (read || write) {
                    return proxy(PreparedStatement.class, (ignoredStatement, action, params) -> {
                        // 3. SQL을 준비한 시점이 아니라 execute 계열 메서드 호출 시 집계
                        if (action.getName().startsWith("execute")) {
                            lease.stock = true;
                            if (read) sample.reads++;
                            if (write) sample.writes++;
                        }
                        return invoke(result, action, params);
                    });
                }
            }

            return result;
        });
    }

    private static <T> T proxy(Class<T> type, InvocationHandler handler){
        return type.cast(Proxy.newProxyInstance(
                type.getClassLoader(),new Class<?>[]{type},handler
        ));
    }

    private static Object invoke(Object target, Method method,Object[] args) throws Throwable{
        try{
            return method.invoke(target, args);
        }catch(InvocationTargetException e){
            throw e.getCause();
        }
    }
}
