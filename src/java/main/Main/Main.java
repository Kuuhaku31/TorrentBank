package Main;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import Database.Database;
import Database.DatabaseRecode;
import Database.FastResumeRecode;
import Database.TorrentRecode;

public class Main {

    private static final String HELP_MSG =
        """
    Usage:
        java -jar TorrentBank.jar <command> [options]                       - 导入或导出 BT_backup 文件到数据库
    
    command:
        import <BT_backup Dir> [--db <Database Path>]                        -导入指定目录下的 BT_backup 文件到数据库，默认数据库路径为当前目录下的 torrent_bank.db
        export <qBittorrent Category> <Export Dir> [--db <Database Path>]   - 导出指定 qBittorrent 分类的 torrent 和 fastresume 文件到指定目录，默认数据库路径为当前目录下的 torrent_bank.db
    """;

    public static void main(String[] args) {
        System.out.println("Hello, TorrentBank!");

        // 参数检查
        if(args.length < 2) {
            System.out.println(HELP_MSG);
            return;
        }

        var command = args[0];
        try {
            switch(command) {
            case "import" -> {
                var btBackupDir = args[1];
                var dbPath      = "torrent_bank.db"; // 默认数据库路径
                // 解析可选参数
                for(int i = 2; i < args.length - 1; i++) {
                    if("--db".equals(args[i])) {
                        dbPath = args[i + 1];
                        break;
                    }
                }
                System.out.println("正在导入 BT_backup 文件...");
                var res = BencodeFileReader.BencodeFileReader.readFiles(btBackupDir);

                List<DatabaseRecode> records = new ArrayList<>();
                for(var fileContent : res.torrentFileContents()) records.add(new TorrentRecode(fileContent));
                for(var fileContent : res.fastResumeFileContents()) records.add(new FastResumeRecode(fileContent));

                try(var db = new Database(dbPath)) {
                    db.upsert(records);
                    System.out.println("导入完成!");
                } catch(Exception e) {
                    System.out.println("导入过程中发生错误: " + e.getMessage());
                    e.printStackTrace();
                }
            }
            case "export" -> {
                if(args.length < 3) {
                    System.out.println("导出命令需要至少 3 个参数");
                    System.out.println(HELP_MSG);
                    return;
                }
                var qbtCategory = args[1];
                var exportDir   = args[2];
                var dbPath     = "torrent_bank.db"; // 默认数据库路径
                // 解析可选参数
                for(int i = 3; i < args.length - 1; i++) {
                    if("--db".equals(args[i])) {
                        dbPath = args[i + 1];
                        break;
                    }
                }
                System.out.println("正在导出分类 [" + qbtCategory + "] 的记录...");
                try(var db = new Database(dbPath)) {

                    // 创建目录
                    var exportPath = Path.of(exportDir);
                    if(!exportPath.toFile().exists()) {
                        System.out.println("导出目录不存在，正在创建: " + exportDir);
                        Files.createDirectories(exportPath);
                    }

                    db.exportByCategory(qbtCategory, Path.of(exportDir));
                    System.out.println("导出完成!");
                } catch(Exception e) {
                    System.out.println("导出过程中发生错误: " + e.getMessage());
                    e.printStackTrace();
                }
            }
            default -> {
                System.out.println("未知的命令: " + command);
                System.out.println(HELP_MSG);
            }
            }
        } catch(Exception e) {
            System.out.println("执行命令时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
