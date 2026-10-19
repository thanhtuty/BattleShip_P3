# BattleShip Online – Phần 3 (Java 17, NetBeans, SQL Server)

Project tách riêng phần game: bàn 6×6, 4 tàu dài 2/3/4/5, đặt ngang/dọc, không chồng/vượt biên; server quản lý Ready, lượt bắn, kết quả Miss/Hit/Sunk và đồng hồ 15 giây. Bắn trúng hoặc đánh chìm được tàu thì giữ lượt; bắn trượt hoặc hết giờ thì chuyển lượt. Chỉ hiện hộp thoại khi bắt đầu trận, chìm tàu, timeout và kết quả cuối. Lời mời, lượt đặt tàu, Ready, mỗi lần bắn, bắt đầu/kết thúc trận đều được ghi SQL Server và có thể xem lại qua nút **LỊCH SỬ**.

> Đây là project minh họa độc lập cho phần 3. Người chơi đăng nhập hoặc tạo tài khoản; tài khoản được lưu trong bảng `UserAccounts` của SQL Server. CSDL tạo riêng `BattleShipPart3`; khi ghép project nhóm, thay tầng kết nối bằng message contract và MatchId/UserId chung của nhóm.

## Luồng mời đấu và chạy
Sau khi hai Client đăng nhập, cả hai ở sảnh. Một người nhấn **MỜI ĐẤU**; người kia chọn đồng ý hoặc từ chối trong hộp thoại. Lời mời hết hạn sau 30 giây. Server chỉ tạo bản ghi trận và mở bước đặt tàu khi lời mời được chấp nhận. Mỗi lần chạy minh họa này nhận đúng hai Client.

Giao diện dùng nền sáng, chữ đậm, màu tàu xanh đậm, ô trúng đỏ và ô trượt vàng để dễ nhìn khi chiếu trong phòng sáng. Nút **LỊCH SỬ** truy vấn SQL và hiển thị 10 trận gần nhất cùng tối đa 40 sự kiện mỗi trận.

## Cài và chạy
1. Cài JDK 17, Maven và SQL Server; bật TCP/IP, đặt SQL Server nghe cổng 1433.
2. Mở `database/01_create_database.sql` trong SSMS, chạy toàn bộ script. Script tạo cả bảng `UserAccounts`. Có thể chạy lại script để bổ sung bảng này vào database đã tạo trước đó. Có thể thay `sa` bằng SQL Login riêng có quyền trên `BattleShipPart3`.
3. Trong NetBeans chọn **File > Open Project**, trỏ tới thư mục có `pom.xml`; chờ Maven tải driver JDBC.
4. Chạy `setup-config.bat`, sửa `db.username`/`db.password`, Ctrl+S. `config/db.properties` bị Git ignore; không commit mật khẩu.
5. Run `battleship.part3.Server` một lần. Server tự tạo hai tài khoản mẫu nếu chưa có: `abc1` / `abc12345` và `abc2` / `abc12345`.
6. Run `battleship.part3.Client` hai lần. Chọn đăng nhập bằng tài khoản mẫu hoặc tạo tài khoản mới. Cùng máy nhập `127.0.0.1`; hai máy nhập IP LAN của máy chạy server. Mở TCP port 5002 trên firewall server.
7. Sau khi một người gửi lời mời và người còn lại chấp nhận, mỗi người đặt lần lượt tàu 2, 3, 4, 5 ô: nhấn Đổi hướng để đổi ngang/dọc rồi bấm ô bắt đầu từng tàu. Khi đủ 4 tàu, nhấn **SẴN SÀNG**. Đánh dấu `■` là tàu của bạn, `×` là ô trúng, `·` là ô trượt.

## Các bảng
- `Matches`: người chơi, trạng thái, lượt hiện tại, người thắng và mốc thời gian.
- `PlayerBoards`: trạng thái bàn cờ, Ready, số phát bắn.
- `GameEvents`: nhật ký lời mời, đặt tàu, READY, START, HIT, MISS, SUNK, WIN và kết nối; Client có thể xem cùng lịch sử trận.
- `UserAccounts`: tên tài khoản, salt và hash PBKDF2 của mật khẩu; mật khẩu không được lưu dạng chữ thường.

## Giao thức TCP đơn giản
Mỗi message là một chuỗi UTF có dạng `TYPE|field...`: `LOGIN|username|password`, `REGISTER|username|password`, `INVITE`, `ANSWER|inviter|YES/NO`, `PLACE|row|col|H/V`, `READY`, `SHOT|row|col`; server trả `PLACE_OK`, `TURN`, `ATTACK_RESULT`, `STATE`, `START`, `SUNK`, `WIN`, `ERROR`. Server là nguồn quyết định trạng thái và kiểm tra người chơi nào được bắn.

## Lớp chính
- `Board`, `Cell`, `Ship`: bàn 6×6, đặt tàu và kiểm tra nước bắn.
- `Game`, `GameManager`, `TurnManager`: luật trận, Ready, đổi lượt và giới hạn 15 giây.
- `Server`: TCP server có thẩm quyền kiểm tra lệnh và phát trạng thái cho hai Client.
- `Client`: giao diện Swing cho thao tác đặt tàu và bắn.
- `DbConfig`, `AccountDAO`, `GameDAO`: đọc cấu hình local, xác thực tài khoản bằng PBKDF2 và ghi trận/bàn cờ/sự kiện bằng JDBC PreparedStatement.

## Kiểm tra luật nhanh
Mở `src/test/java/battleship/part3/SelfTest.java` và chọn **Run File** trong NetBeans. Kết quả mong đợi: `SelfTest OK`; bài test này không cần kết nối SQL.

## Ghi chú
- `config/db.properties.example` là mẫu; không sửa rồi commit mật khẩu thật.
- Client/Server dùng TCP thường, phù hợp demo trong LAN, không dùng cho tài khoản/mật khẩu thật trên Internet.
- Project cần quyền truy cập Maven Central để tải Microsoft JDBC Driver lần đầu.
