# 👜 SanSam Backend #

SanSam은 명품 이커머스 플랫폼을 가정한 백엔드 서비스입니다.
이 저장소는 실제 트래픽·동시성·장애 상황을 고려한 주문–결제–재고 시스템 설계를 목표로 구현되었습니다.

📌 본 문서는 Git 작성자 기준으로, 제가 직접 설계·구현한 모듈만을 중심으로 정리되어 있습니다.

## ✍️ Author & Scope ##
Git Author: xeulbn (gsbtiger0215@gmail.com)

## 담당 영역 ##
- 주문 (Order)
- 결제 (Payment)
- 재고 (Stock)
- 상태 (Status)

## 🛠 Tech Stack ##
###Language / Framework###
- Java 21
- Spring Boot 3.5.x
- Persistence
- Spring Data JPA
- QueryDSL
- MySQL
- Infra / Middleware
- Redis
- Spring Batch
- Spring Scheduler

### Test ###
JUnit 5
Jacoco Test Coverage Report

--
## 📦 Implemented Modules ##
🧾 주문 (Order)
- 주문 생성 트랜잭션 구성
- 주문번호 생성 정책 (OrderNumberPolicy)
- 가격 계산 정책 분리 (PricingPolicy)
- 주문 / 주문상품 상태 전이 관리
- 주문 만료 처리 및 재고 복구 보상 트랜잭션 (Outbox 패턴) 설계

## 💳 결제 (Payment) ##
- Toss Payments 결제 승인 흐름 구현
- 외부 결제 응답 정규화 (Normalize Layer)
- 결제 승인 멱등성 보장
- 결제 실패 시 Best-effort 취소 → 실패 시 Outbox 큐잉
- 부분 취소 / 전체 취소 상태 전이 및 이력 관리

## 📦 재고 (Stock) ##
- Redis 기반 재고 선점(Reserve) 구조
- Lua Script 기반 원자적 재고 차감
- 재고 부족 사전 검증
- Redis 분산락 유틸리티 구현
- 일일 사용량 집계 → RDB 동기화 스케줄러
- Redis / DB 메트릭 수집 구조

## 🔁 상태 (Status) ##
- 상태 코드 캐싱 서비스 (@Cacheable)
- 상태 변경 시 캐시 갱신 로직 분리
- 주문 / 결제 / 재고 상태 전이 공통 관리
--
## 🔄 Core Flow ##

### 1️⃣ 주문 생성 ###
AfterConfirmOrderService.placeOrderTransaction
- Redis 재고 선점
- 주문 / 주문상품 생성
- 주문번호 정책 적용
- 가격 정책 적용
- 단일 트랜잭션 내 처리

### 2️⃣ 결제 승인 ###
PaymentService.confirmPayment
- 주문 및 금액 검증
- Toss 결제 승인 요청
- 응답 정규화
- AfterConfirmTransactionService.approveInTransaction 호출
- DB 실패 시 Best-effort 결제 취소
- 취소 실패 시 Outbox 큐잉 → 비동기 보상 처리

### 3️⃣ 주문 만료 & 재고 복구 ###
OrderCleanUpScheduler
- 만료 주문 탐색
- OrderExpiryProcessor
- 주문 상태 만료 처리
- 재고 복구 Outbox enqueue
- StockRestoreWorker / Processor
- 재고 복구 비동기 처리

### 4️⃣ 재고 동기화 ###
RedisStockService.reserve
- Lua Script 기반 원자적 재고 선점
- StockSyncScheduler
- Redis 일일 사용량 집계
- RDB 동기화

--
## 🗂 Project Structure ##

src/main/java/org/example/sansam
-> order      # 주문 도메인
-> payment    # 결제 도메인
-> stock      # 재고 도메인
-> status     # 상태/코드 관리
 
--

## 🚀 Run ##
./gradlew bootRun

## 🧪 Test ##
./gradlew test

--

## 🎯 Design Focus ##
1. 동시성 환경에서의 재고 정합성
2. 외부 결제 실패를 고려한 보상 트랜잭션
3. 트랜잭션 경계 명확화
4. Outbox 패턴 기반 비동기 복구
5. 상태 전이의 명시적 모델링
6. “성공 흐름보다 실패 흐름”을 먼저 설계
