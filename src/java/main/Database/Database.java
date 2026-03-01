package Database;

import java.io.Closeable;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;


public class Database implements Closeable {

    private final Connection conn; // SQLite 数据库连接
    private static final int DEFAULT_BATCH_SIZE = 500; // 默认批处理大小

    private static final String UPSERT_TORRENT_SQL = 
    """
    INSERT INTO torrent (TOR_HASH, file_name, file_size, torrent_file)
    VALUES (?, ?, ?, ?)
    ON CONFLICT(TOR_HASH) DO UPDATE SET
        file_name = excluded.file_name,
        file_size = excluded.file_size,
        torrent_file = excluded.torrent_file;
    """;

    private static final String UPSERT_FAST_RESUME_SQL = 
    """
    INSERT INTO torrent (TOR_HASH, qbt_category, fastresume)
    VALUES (?, ?, ?)
    ON CONFLICT(TOR_HASH) DO UPDATE SET
        qbt_category = excluded.qbt_category,
        fastresume = excluded.fastresume;
    """;

    private final PreparedStatement psUpsertTorrent;
    private final PreparedStatement psUpsertFastResume;

    private static void ensureSqliteDriverLoaded() {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("未找到 SQLite JDBC 驱动，请确认已添加 org.xerial:sqlite-jdbc 依赖", e);
        }
    }

    public Database(String dbPath) throws Exception {
        ensureSqliteDriverLoaded();
        // 确保数据库文件存在
        var dbUrl = "jdbc:sqlite:" + dbPath;
        if(!new File(dbPath).exists()) {
            System.out.println("该数据库文件不存在: " + dbPath);
            System.out.println("正在创建数据库...");
            initDatabase(dbUrl); // 如果表不存在则创建表
        }
        conn = DriverManager.getConnection(dbUrl); // 连接 SQLite 数据库

        // 预编译 SQL 语句
        psUpsertTorrent    = conn.prepareStatement(UPSERT_TORRENT_SQL);
        psUpsertFastResume = conn.prepareStatement(UPSERT_FAST_RESUME_SQL);
    }

    public void upsert(List<DatabaseRecode> records) throws SQLException {

        // 如果没有项目需要处理，则直接返回
        if(records == null || records.isEmpty()) return;

        // 关闭自动提交以启用批处理
        var prevAuto = conn.getAutoCommit(); // 保存之前的自动提交状态
        conn.setAutoCommit(false);           // 关闭自动提交

        // 根据不同类型使用不同SQL语句
        int torrentCount = 0, fastResumeCount = 0;
        for (var record : records) {

            // torrent记录
            if(record instanceof TorrentRecode) {
                var torrentRecord = (TorrentRecode) record;
                safeSetString(psUpsertTorrent, 1, torrentRecord.TOR_HASH);
                safeSetString(psUpsertTorrent, 2, torrentRecord.file_name);
                safeSetLong(psUpsertTorrent, 3, torrentRecord.file_size);
                safeSetBytes(psUpsertTorrent, 4, torrentRecord.torrentFileContent);
                psUpsertTorrent.addBatch();
                torrentCount++;

                if(torrentCount % DEFAULT_BATCH_SIZE == 0) psUpsertTorrent.executeBatch();
            }
            
            // fastresume记录
            else if(record instanceof FastResumeRecode) {
                var fastResumeRecord = (FastResumeRecode) record;
                safeSetString(psUpsertFastResume, 1, fastResumeRecord.TOR_HASH);
                safeSetString(psUpsertFastResume, 2, fastResumeRecord.category);
                safeSetBytes(psUpsertFastResume, 3, fastResumeRecord.fastResumeFileContent);
                psUpsertFastResume.addBatch();
                fastResumeCount++;

                if(fastResumeCount % DEFAULT_BATCH_SIZE == 0) psUpsertFastResume.executeBatch();
            }

            else {
                throw new IllegalArgumentException("Unsupported record type: " + record.getClass().getName());
            }
        }

        // 执行批处理操作
        if(torrentCount > 0) psUpsertTorrent.executeBatch();
        if(fastResumeCount > 0) psUpsertFastResume.executeBatch();

        // 提交事务并恢复之前的自动提交状态
        conn.commit();
        conn.setAutoCommit(prevAuto);
    }

    @Override
    public void close() {
        try {
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initDatabase(String dbUrl) throws Exception {
        try (
            var conn = DriverManager.getConnection(dbUrl);  
            var stmt = conn.createStatement()
        ) {
            String sql = 
            """
            CREATE TABLE "torrent" (

                "TOR_HASH"     text NOT NULL,

                "file_name"    text,
                "file_size"    integer,
                "qbt_category" text,

                "torrent_file" blob,
                "fastresume"   blob,

                PRIMARY KEY ("TOR_HASH" DESC)
            );
            """;;
            stmt.execute(sql);
        }
    }

    private static void safeSetString(PreparedStatement ps, int index, String value) throws SQLException {
        if (value == null) {
            ps.setNull(index, java.sql.Types.VARCHAR);
        } else {
            ps.setString(index, value);
        }
    }

    private static void safeSetLong(PreparedStatement ps, int index, Long value) throws SQLException {
        if (value == null) {
            ps.setNull(index, java.sql.Types.BIGINT);
        } else {
            ps.setLong(index, value);
        }
    }

    private static void safeSetBytes(PreparedStatement ps, int index, byte[] value) throws SQLException {
        if (value == null) {
            ps.setNull(index, java.sql.Types.BLOB);
        } else {
            ps.setBytes(index, value);
        }
    }
}
