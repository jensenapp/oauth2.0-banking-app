# 銀行後端系統 (Banking Backend System)

下面提供 **「面試用 Java 後端專案 README（超優、企業級版本）」**，你可以直接放進 GitHub，整體敘述偏專業、清楚又不會太浮誇，能有效提升面試官好印象。

---

# 🏦 Banking Service API

**A secure, production-like banking system built with Spring Boot, Spring Security, Keycloak, and JPA**
此專案模擬「銀行帳戶管理系統」，具備完整驗證機制、交易流程、併發控制、資安考量與 REST API 設計，適合後端工程師面試作品。

---

## 🚀 專案特色（重點亮點）

本專案強調企業級後端設計思維：

### **1. 完整的 JWT OAuth2 安全架構（由 Keycloak 驗證）**

* 採用 Authorization Code + PKCE（最常見於真實前後端分離架構）
* Access Token & Refresh Token 皆於 Keycloak 管理
* 系統內部不自行驗證密碼（符合零信任架構）

### **2. 支援多帳戶管理 + 交易紀錄**

* 開戶、查詢餘額、存款、提款、轉帳
* 所有操作建立 Transaction Log

### **3. 高併發存提款安全機制**

* 使用 **樂觀鎖（@Version）**
* 存提款自動重試機制（retry up to 3 times）
* 避免高併發下資金錯誤

### **4. 轉帳使用資料庫悲觀鎖（SELECT FOR UPDATE）**

* 防止 Deadlock
* 保證從帳戶 A → 帳戶 B 金流一致性

### **5. 完整 DTO 分層、Validation、Mapper 分離**

* 防止 entity 直接暴露
* 問題追蹤與維護更清楚

### **6. Logging 企業級處理**

* SLF4J + 一致性 Log Template
* 清楚紀錄每次交易流程

---

## 🧩 系統架構圖

```
Client → Keycloak → Spring Boot API → Service Layer → Repository → MySQL/H2
```

---

## 📦 技術棧 (Tech Stack)

| 技術                                    | 用途                 |
| ------------------------------------- | ------------------ |
| **Java 17 / 21**                      | Backend 主語言        |
| **Spring Boot 3 / Spring Security 6** | REST API、驗證、授權     |
| **OAuth2 + JWT (Keycloak)**           | Token 驗證           |
| **Spring Data JPA / Hibernate**       | ORM、交易控制、樂觀鎖/悲觀鎖   |
| **H2 / MySQL**                        | Database           |
| **Docker Compose**                    | 一鍵啟動：Keycloak + DB |
| **Lombok / Record**                   | 精簡程式碼              |
| **Validation API**                    | 參數驗證               |

---

## 📁 資料夾結構（重點版）

```
src/main/java/net/javaguides/banking
 ├── dto/
 ├── entity/
 ├── mapper/
 ├── service/
 ├── repository/
 └── exception/
```

---

## 🔐 身份驗證流程（簡化說明）

1. 前端使用 Authorization Code + PKCE 向 Keycloak 授權
2. 取得 Access Token
3. Backend 使用 Spring Security 自動驗證 JWT
4. 從 JWT Claims 取：

    * `preferred_username`
    * `name`
    * `sub (userId)`
5. Backend 不需要知道密碼（由 Keycloak 管理）

---

## 🏛 功能說明

### ✔ 1. 開戶 API

依 JWT 內的使用者資訊自動建立帳戶。

### ✔ 2. 存款 / 提款

* 存款：加金額 → 紀錄交易 Log
* 提款：檢查餘額 → 扣款 → 交易紀錄
* 皆有樂觀鎖 + 重試

### ✔ 3. 轉帳

* 兩個帳戶 lock（依 ID 大小排序）避免 Deadlock
* 金額從 A 減 → B 加
* 建立轉帳紀錄（Transfer_in, Transfer_out）

### ✔ 4. 查詢帳戶列表（分頁）

使用 Page<T> 結構輸出 PageResponseDTO

---

## 🧪 API 範例

### **POST /api/accounts**

建立帳戶（須登入）

```json
{
  "balance": 1000
}
```

### **POST /api/accounts/{id}/deposit**

```json
{
  "amount": 500
}
```

### **POST /api/accounts/transfer**

```json
{
  "fromAccountId": 1,
  "toAccountId": 2,
  "amount": 150
}
```

---

## 🧑‍💻 如何啟動專案

### 1. Clone 專案

```bash
git clone https://github.com/your-repo/banking-service.git
cd banking-service
```

### 2. 啟動 Keycloak（若使用 Docker Compose）

```bash
docker compose up -d
```

### 3. 啟動 Spring Boot

```bash
./mvnw spring-boot:run
```

---

##  Todo

*  Transaction 查詢 API
*  使用者權限（Admin/User）
*  Swagger API Docs
*  Redis Cache（存交易紀錄）

