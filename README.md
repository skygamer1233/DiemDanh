# 📅 DiemDanh Reloaded

<p align="center">
  <img src="https://img.shields.io/badge/Version-1.5--BETA-brightgreen.svg" alt="Version">
  <img src="https://img.shields.io/badge/Java-21-orange.svg" alt="Java 21">
  <img src="https://img.shields.io/badge/Minecraft-1.13_to_1.21.x-blue.svg" alt="Minecraft">
  <img src="https://img.shields.io/badge/Platform-Paper%20%7C%20Spigot%20%7C%20Purpur-lightgrey.svg" alt="Platform">
  <img src="https://img.shields.io/badge/Author-SkyGamer-purple.svg" alt="Author">
</p>

**DiemDanh Reloaded** là một plugin điểm danh hàng ngày chuyên nghiệp, tối ưu và toàn diện dành cho máy chủ Minecraft (Spigot / Paper / Purpur từ phiên bản 1.13 đến 1.21+). Plugin cung cấp giao diện GUI dạng lịch tháng trực quan, hệ thống phần thưởng tích lũy, điểm danh bù, sự kiện ngày lễ đặc biệt, bảng xếp hạng Top và trình biên tập GUI Editor in-game tiện lợi.

---

## ✨ Tính Năng Nổi Bật

- 📆 **Giao diện Lịch Điểm Danh Động:**
  - Tự động nhận diện số ngày thực tế của tháng (28, 29, 30 hoặc 31 ngày) bằng `YearMonth`.
  - Phân loại trạng thái ngày trực quan: *Chưa tới ngày, Hôm nay có thể điểm danh, Đã điểm danh, Bỏ lỡ, Điểm danh bù*.
- 🎁 **Mốc Quà Tích Lũy (Streak Milestones):**
  - Khuyến khích người chơi duy trì điểm danh với các mốc thưởng lớn (7 ngày, 14 ngày, 21 ngày).
- 🎫 **Phiếu Điểm Danh Bù (Compensation Tickets):**
  - Cho phép người chơi sử dụng vé điểm danh bù để gỡ lại những ngày đã lỡ trong tháng.
- 🎉 **Sự Kiện Ngày Lễ Đặc Biệt (SpecialDay):**
  - Tùy biến phần thưởng và icon riêng biệt cho các ngày lễ cố định (như 1/6, 2/9,...).
- 🏆 **Bảng Xếp Hạng Top 10 Siêu Mượt (Leaderboard GUI):**
  - Xem Top 10 Tháng và Top 10 Tổng tích lũy hiển thị đầu người chơi (`PLAYER_HEAD`).
  - **Tối ưu Asynchronous:** Tự động tính toán ngầm và cache dữ liệu định kỳ mỗi 5 phút, mở GUI **0ms độ trễ**, hoàn toàn không gây khựng/drop TPS máy chủ.
- 🛠 **Trình Biên Tập GUI In-Game (`/diemdanh editor`):**
  - Admin có thể trực tiếp thêm/sửa/xóa lệnh thưởng hoặc tạo ngày lễ mới qua giao diện và chat mà không cần sửa file YAML thủ công.
- ⚡ **Lưu Trữ Bất Đồng Bộ Độc Lập (Async UUID Storage):**
  - Dữ liệu người chơi lưu theo từng file riêng biệt trong thư mục `playerdata/<UUID>.yml`.
  - Cơ chế lưu ngầm bất đồng bộ (Async I/O) giúp server hoạt động mượt mà ngay cả khi có hàng nghìn người chơi.
  - Tự động chuyển đổi dữ liệu từ file `playerdata.yml` cũ sang cấu trúc mới an toàn 100%.
- 🔒 **Bảo Mật & Chống Lỗi Đồ (Anti-Glitch):**
  - Sử dụng hệ thống `InventoryHolder` chuẩn hóa.
  - Khóa toàn bộ hành vi kéo thả (`InventoryDragEvent`) và spam click cooldown, chống triệt để mọi nguy cơ đúp đồ/lấy item GUI.
- 🌐 **Hỗ Trợ PlaceholderAPI & Đa Ngôn Ngữ:**
  - Tích hợp sẵn expansion cho PlaceholderAPI dùng cho Scoreboard, Tablist, Chat.
  - Hỗ trợ đầy đủ mã màu Hex (`&#RRGGBB`) và mã màu chuẩn (`&a`, `&b`,...).
  - Đa ngôn ngữ: Tiếng Việt (`message_vi.yml`) và Tiếng Anh (`message_en.yml`).

---

## 📋 Lệnh & Quyền Hạn (Commands & Permissions)

| Lệnh | Mô tả | Quyền hạn | Mặc định |
| :--- | :--- | :--- | :--- |
| `/diemdanh` | Mở giao diện lịch điểm danh cá nhân | `diemdanh.use` | `true` (Tất cả) |
| `/diemdanh top` | Mở bảng xếp hạng Top điểm danh | `diemdanh.top` | `true` (Tất cả) |
| `/diemdanh giveticket <player> <số_lượng>` | Tặng vé điểm danh bù cho người chơi | `diemdanh.giveticket` | `OP` |
| `/diemdanh editor` | Mở giao diện cấu hình phần thưởng in-game | `diemdanh.editor` | `OP` |
| `/diemdanh reload` | Nạp lại toàn bộ file cấu hình và ngôn ngữ | `diemdanh.reload` | `OP` |

---

## 🧩 Placeholders (PlaceholderAPI)

Nếu máy chủ của bạn cài đặt [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/), bạn có thể sử dụng các placeholder sau:

### 👤 Thông tin cá nhân người chơi

| Placeholder | Mô tả | Kết quả mẫu |
| :--- | :--- | :--- |
| `%diemdanh_days%` | Số ngày đã điểm danh trong tháng này | `15` |
| `%diemdanh_totaldays%` | Tổng số ngày đã điểm danh từ trước tới nay | `48` |
| `%diemdanh_tickets%` | Số lượng vé điểm danh bù hiện có | `3` |
| `%diemdanh_checked_today%` | Trạng thái điểm danh hôm nay (có màu) | `&aĐã điểm danh` / `&cChưa điểm danh` |
| `%diemdanh_is_checked_today%` | Trạng thái boolean hôm nay | `true` / `false` |

### 🏆 Thông tin Bảng Xếp Hạng

| Placeholder | Mô tả | Kết quả mẫu |
| :--- | :--- | :--- |
| `%diemdanh_top_month_<1-10>_name%` | Tên người chơi Top Tháng tại vị trí chỉ định | `SkyGamer` |
| `%diemdanh_top_month_<1-10>_days%` | Số ngày điểm danh Top Tháng tại vị trí chỉ định | `25` |
| `%diemdanh_top_total_<1-10>_name%` | Tên người chơi Top Tổng tại vị trí chỉ định | `Player_1` |
| `%diemdanh_top_total_<1-10>_days%` | Tổng ngày điểm danh Top Tổng tại vị trí chỉ định | `120` |

*Ví dụ:* `%diemdanh_top_month_1_name%` trả về tên người chơi Top 1 tháng hiện tại.

---

## 📁 Cấu Trúc Thư Mục Plugin

```
plugins/DiemDanh/
 ├── config.yml           # Cấu hình quà từng ngày, mốc tích lũy và ngày lễ
 ├── editor.yml           # Cấu hình giao diện và tin nhắn của Menu Editor
 ├── topgui.yml           # Cấu hình giao diện bảng xếp hạng Top
 ├── language/
 │    ├── message_vi.yml  # File ngôn ngữ Tiếng Việt
 │    └── message_en.yml  # File ngôn ngữ Tiếng Anh
 └── playerdata/          # Dữ liệu điểm danh của từng người chơi lưu theo UUID
      ├── <uuid-1>.yml
      └── <uuid-2>.yml
```

---

## ⚙️ Hướng Dẫn Cài Đặt

1. **Yêu cầu hệ thống:**
   - **Java:** Phiên bản 21 trở lên.
   - **Máy chủ:** Paper, Purpur hoặc Spigot (1.13 - 1.21.x).
   - *(Khuyến nghị)* [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) để hiển thị thông tin lên Scoreboard hoặc Tablist.
2. Tải file `diemdanh-1.5-BETA.jar` từ mục **Releases** trên GitHub.
3. Bỏ file `.jar` vào thư mục `plugins/` của máy chủ.
4. Khởi động lại máy chủ (hoặc dùng PlugMan / lệnh khởi động).
5. Tùy chỉnh quà tặng và giao diện trong thư mục `plugins/DiemDanh/` theo ý muốn!

---

## 🛠 Hướng Dẫn Biên Dịch Từ Mã Nguồn (Build from Source)

Dành cho các lập trình viên muốn tự biên dịch hoặc đóng góp cho dự án:

```bash
# Clone repository
git clone https://github.com/<your-username>/DiemDanh.git
cd DiemDanh

# Biên dịch bằng Maven
mvn clean package
```

File JAR hoàn chỉnh sẽ nằm trong thư mục `target/diemdanh-1.5-BETA.jar`.

---

## 👨‍💻 Tác Giả & Bản Quyền

- **Phát triển bởi:** [SkyGamer](https://github.com/SkyGamer)
- **Phiên bản:** `1.5-BETA`
- Nếu bạn thấy plugin hữu ích, hãy để lại một ⭐ trên repository để ủng hộ tác giả nhé!
