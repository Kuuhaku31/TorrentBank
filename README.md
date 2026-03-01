# TorrentBank

从 qBittorrent 导出种子并保存到数据库中。

基于 SQLite 数据库。

## qBittorrent

.fastresume 文件是 qBittorrent 用来保存种子状态的文件，包含了种子的哈希值、文件名、文件大小等信息。

### BencodeParser 类

方法:

```java
// 构造函数，接受 Bencode编码的数据作为输入
// 可能是种子文件的内容，也可能是 .fastresume 文件的内容
BencodeParser(byte[] fileContent)

// 尝试提取出种子哈希值，失败返回 null
String getHashFromTorrent()

// 尝试提取出种子文件名，失败返回 null
String getFileNameFromTorrent()

// 尝试提取出 qBt-category 字段内容，失败返回 null
String getCategoryFromFastResume()
```

### DataBase 类

基于 SQLite 数据库的简单封装类。

```java
// 构造函数，接受数据库文件路径作为输入
// 如果数据库文件不存在，会自动创建
// 检查表结构，如果结构不正确，抛出异常
DataBase(String dbFilePath) throws SQLException

// 关闭数据库
void close()
```

## 数据库结构

`torrent` 表

| 键名           | 数据类型 | 解释             | 备注       |
| -------------- | -------- | ---------------- | ---------- |
| `TOR_HASH`     | text     | 种子 info_hash   | 主键       |
| -------------- | -------- | ---------------- | ---------- |
| `file_name`    | text     | 文件名称         |            |
| `file_size`    | integer  | 文件大小         | （字节）   |
| `qbt_category` | text     | qBittorrent 分类 |            |
| -------------- | -------- | ---------------- | ---------- |
| `torrent_file` | blob     | 种子文件         | 二进制数据 |
| `fastresume`   | blob     | .fastresume 文件 | 二进制数据 |

```sql
CREATE TABLE "torrent" (

    "TOR_HASH"     text NOT NULL,

    "file_name"    text,
    "file_size"    integer,
    "qbt_category" text,

    "torrent_file" blob,
    "fastresume"   blob,

    PRIMARY KEY ("TOR_HASH" DESC)
);
```
