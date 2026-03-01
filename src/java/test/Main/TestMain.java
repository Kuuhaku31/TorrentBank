package Main;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import Database.DatabaseRecode;
import Database.FastResumeRecode;
import Database.TorrentRecode;


public class TestMain {
    public static void main(String[] args) throws IOException {
        System.out.println("Test Main");

        var res = BencodeFileReader.BencodeFileReader.readFiles("C:/Users/kuuhaku/AppData/Local/qBittorrent/BT_backup");

        List<DatabaseRecode> records = new ArrayList<>();
        for(var fileContent : res.torrentFileContents()) records.add(new TorrentRecode(fileContent));
        for(var fileContent : res.fastResumeFileContents()) records.add(new FastResumeRecode(fileContent));

        // 获取工作目录
        String pwd = System.getProperty("user.dir");

        // 数据库测试
        var dbPath = pwd + "/ignore/test.db";
        try(var db = new Database.Database(dbPath)) {
            db.upsert(records);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
