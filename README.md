# BattleShip Online – Phần 3 (Java 17, NetBeans, SQL Server)

Project Phần 3 dùng Maven, Swing, TCP và SQL Server. Bàn cờ 6×6 có bốn tàu dài 2, 3, 4, 5 ô. Tàu đặt ngang/dọc, không chồng nhau và không vượt biên. Trúng thì giữ lượt; trượt hoặc hết 15 giây thì đổi lượt. Server xác nhận nước bắn và kết quả trận.

## Cấu trúc mã nguồn

```text
src/main/java/battleship/part3/
├── client/
│   ├── Client.java
│   └── GameClient.java
└── server/
    ├── Server.java
    ├── AccountDAO.java
    ├── DbConfig.java
    ├── GameDAO.java
    ├── Board.java
    ├── Cell.java
    ├── Ship.java
    ├── Game.java
    ├── GameManager.java
    └── TurnManager.java
```

- `client`: giao diện Swing và kết nối TCP. `GameClient` dùng `DataInputStream.readUTF()` / `DataOutputStream.writeUTF()`, khớp giao thức hiện có.
- `server`: nhận lệnh, kiểm tra luật và truy cập database. Client gửi thao tác; Server kiểm tra rồi trả trạng thái.
- Các lớp luật game đặt trong package `server`; Client chỉ dùng `Board` để xem trước vị trí đặt tàu, Server vẫn kiểm tra lại khi nhận lệnh.

## Cài và chạy

1. Cài JDK 17, Maven và SQL Server; bật TCP/IP và cấu hình cổng SQL Server.
2. Mở `database/01_create_database.sql` trong SSMS và chạy toàn bộ script.
3. Mở project bằng NetBeans tại thư mục có `pom.xml`.
4. Chạy `setup-config.bat`, nhập thông tin SQL Server vào `config/db.properties` rồi lưu.
5. Chạy `run-server.bat` trước. Server mặc định nghe cổng 5002.
6. Chạy `run-client.bat` hai lần. Cùng máy dùng `127.0.0.1`; máy khác dùng IP LAN của máy chạy Server.
7. Đăng nhập bằng `abc1 / abc12345` và `abc2 / abc12345`, hoặc tạo tài khoản mới. Sau khi lời mời được chấp nhận, hai người đặt tàu, Ready rồi chơi.

Có thể chạy bằng Maven:

- Server: `mvn compile exec:java -Dexec.mainClass=battleship.part3.server.Server`
- Client: `mvn compile exec:java -Dexec.mainClass=battleship.part3.client.Client`

## Database

Script tạo database `BattleShipPart3` và các bảng `UserAccounts`, `Matches`, `PlayerBoards`, `GameEvents`. Tài khoản lưu salt và hash PBKDF2, không lưu mật khẩu dạng chữ thường. Cấu hình local trong `config/db.properties` không được commit.

## Kiểm tra luật

Trong NetBeans, mở `src/test/java/battleship/part3/server/SelfTest.java` và chọn **Run File**. Bài kiểm tra luật không cần SQL Server.
