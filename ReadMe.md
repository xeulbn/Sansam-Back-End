# 🕒 SanSam's Ecumus Platform

이 프로젝트는 **명품 판매 이커머스 플랫폼**입니다.  
사용자는 명품들만 모여있는 쇼핑몰을 온라인으로 경험할 수 있습니다.
초고가 상품으로 명품들로만 모여있는 쇼핑몰을 경험시켜 드립니다.
일반적인 상품 구입, 리뷰 작성, 위시리스트 등록, 실시간 채팅 등 
다양한 커머스 기능을 제공합니다.

---
## 팀소개
<img width="669" height="319" alt="image" src="https://github.com/user-attachments/assets/d7cd5382-a968-4be7-b7a8-7c32c5284e38" />

---
## Infra
<img width="723" height="427" alt="image" src="https://github.com/user-attachments/assets/70c18b4f-ee3a-43bc-a47b-175a7f2a5cc1" />

---
## ERD
<img width="4320" height="2382" alt="HotDealPlatform (7)" src="https://github.com/user-attachments/assets/408df384-e3d3-401a-8013-834373535544" />

---
## 🚀 주요 기능

### ✅ 사용자 (User)
- 회원가입
- 마이페이지 정보 수정 및 탈퇴
- 세션 기반 로그인

---
## 은경

### ✅ 상품 (Product)
- 상품 등록/조회/삭제 (관리자)
- 검색, 정렬, 카테고리 조회
- 위시리스트 등록/해제

### ✅ 장바구니 (Cart)
- 상품 담기 / 삭제 / 목록 조회

### ✅ 리뷰 (Review)
- 구매 상품에 대한 리뷰 작성/수정/삭제
- 상품 상세 페이지에서 리뷰 전체 조회

---
## 슬빈

## 아키텍처 구성도 (Kafka Streams 도입 이후)
<img width="3258" height="3840" alt="kafkaStreams도입이후_아키텍처" src="https://github.com/user-attachments/assets/b482e91a-b83f-4ef1-a3a5-cf322177a367" />

### ✅ 주문 및 결제 (Order / Payment)
- 주문서 저장 / 삭제
- 토스 API 연동 결제/결제 취소
- 보상 로직

### ✅ 재고 (Stock)
- 재고 차감
- 재고 증가
- 보상 로직

### ✅ 상태 (Status)
- DB에서 이미 저장되어있는 상태 찾기
- 상태값 캐싱

### 프로젝트 여정기

# 🎋 SanSam의 여정기 #
******************

## 😆 주문 / 결제 / 상태 / 재고 발전 과정 (1차 MVP 이후부터 마무리까지) ##
- [👨🏻‍💻 1. 재고 ERD 분리 과정](https://github.com/SanSam2/Back-End/wiki/%EC%9E%AC%EA%B3%A0-ERD-%EB%B6%84%EB%A6%AC-%EA%B3%BC%EC%A0%95)
- [👨🏻‍💻 2. Next-Key Lock을 통한 재고 관리](https://github.com/SanSam2/Back-End/wiki/Next-Key-Lock%EC%9D%84-%ED%86%B5%ED%95%9C-%EC%9E%AC%EA%B3%A0-%EA%B4%80%EB%A6%AC)
- [👨🏻‍💻 3. 테스트 코드 작성과 보상 Outbox패턴 구현기](https://github.com/SanSam2/Back-End/wiki/%ED%85%8C%EC%8A%A4%ED%8A%B8-%EC%BD%94%EB%93%9C-%EC%9E%91%EC%84%B1%EA%B3%BC-%EB%B3%B4%EC%83%81-Outbox%ED%8C%A8%ED%84%B4-%EA%B5%AC%ED%98%84%EA%B8%B0)
- [👨🏻‍💻 4. Order 트랜잭션 분리 & DB Indexing을 통한 최적화 & 테스트 툴 변경](https://github.com/SanSam2/Back-End/wiki/Order-%ED%8A%B8%EB%9E%9C%EC%9E%AD%EC%85%98-%EB%B6%84%EB%A6%AC-%26-DB-Indexing%EC%9D%84-%ED%86%B5%ED%95%9C-%EC%B5%9C%EC%A0%81%ED%99%94-%26-%ED%85%8C%EC%8A%A4%ED%8A%B8-%ED%88%B4-%EB%B3%80%EA%B2%BD)
- [👨🏻‍💻 5. HikariCP 튜닝](https://github.com/SanSam2/Back-End/wiki/HikariCP-%ED%8A%9C%EB%8B%9D)
- [👨🏻‍💻 6. 상태값 캐싱](https://github.com/SanSam2/Back-End/wiki/%EC%83%81%ED%83%9C%EA%B0%92-%EC%BA%90%EC%8B%B1)
- [👨🏻‍💻 7. 재고 비동기 처리 (단일 서버 내)](https://github.com/SanSam2/Back-End/wiki/%EC%9E%AC%EA%B3%A0-%EB%B9%84%EB%8F%99%EA%B8%B0-%EC%B2%98%EB%A6%AC-%28%EB%8B%A8%EC%9D%BC-%EC%84%9C%EB%B2%84-%EB%82%B4%29)
- [👨🏻‍💻 8. 단일 서버의 한계를 해결하기 위해 재고 서버 분리](https://github.com/SanSam2/Back-End/wiki/%EB%8B%A8%EC%9D%BC-%EC%84%9C%EB%B2%84%EC%9D%98-%ED%95%9C%EA%B3%84%EB%A5%BC-%ED%95%B4%EA%B2%B0%ED%95%98%EA%B8%B0-%EC%9C%84%ED%95%B4-%EC%9E%AC%EA%B3%A0-%EC%84%9C%EB%B2%84-%EB%B6%84%EB%A6%AC)
- [👨🏻‍💻 9. RabbitMQ와 Kafka 비교, 그리고 Kafka를 고려한 이유](https://github.com/SanSam2/Back-End/wiki/RabbitMQ%EC%99%80-Kafka-%EB%B9%84%EA%B5%90%2C-%EA%B7%B8%EB%A6%AC%EA%B3%A0-Kafka%EB%A5%BC-%EA%B3%A0%EB%A0%A4%ED%95%9C-%EC%9D%B4%EC%9C%A0)
- [👨🏻‍💻 10. 재고 처리 아키텍처 개선 여정 (마지막 여정)](https://github.com/SanSam2/Back-End/wiki/%EC%9E%AC%EA%B3%A0-%EC%B2%98%EB%A6%AC-%EC%95%84%ED%82%A4%ED%85%8D%EC%B2%98-%EA%B0%9C%EC%84%A0-%EC%97%AC%EC%A0%95)



---
## 호상

### ✅ 알림 시스템 (Notification)
- 결제 완료/취소 이메일 및 홈페이지 알림
- 리뷰 요청 / 재고 부족 알림

---
## 한섭

### ✅ 채팅 (Chat)
- 조건 기반 오픈 채팅방 생성/입장/퇴장
- 실시간 메시지 전송 및 수신
- 채팅 읽음 처리 및 배지 알림
