package org.example.sansam.stock.service;


import lombok.RequiredArgsConstructor;
import org.example.sansam.stock.redis.RedisStockService;
import org.example.sansam.stock.repository.StockRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class StockService {

    private final StockRepository stockRepository;
    private final RedisStockService redisStockService;

    @Transactional
    public void increaseStock(Long productDetailId, int quantity) {
        stockRepository.increase(productDetailId, quantity);
    }

    public void reserveAll(Map<Long, Integer> quantitiesByDetailId) {
        redisStockService.reserveAll(quantitiesByDetailId);
    }

}
