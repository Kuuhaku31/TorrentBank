package BencodeFileReader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class BencodeFileReader {

    public record InnerBencodeFileReader(
        List<byte[]> torrentFileContents,
        List<byte[]> fastResumeFileContents
    ) {}

    // 读取指定文件夹下所有.torrent 和 .fastresume 文件的内容，并返回一个包含这些内容的列表
    public static InnerBencodeFileReader readFiles(String directoryPath) throws IOException {

        List<byte[]> torrentFileContents = new ArrayList<>();
        List<byte[]> fastResumeFileContents = new ArrayList<>();

        Files.walk(Paths.get(directoryPath))
            .filter(Files::isRegularFile)
            .forEach(path -> {
                try {
                    if (path.toString().endsWith(".torrent")) {
                        torrentFileContents.add(Files.readAllBytes(path));
                    } else if (path.toString().endsWith(".fastresume")) {
                        fastResumeFileContents.add(Files.readAllBytes(path));
                    }
                } catch (IOException e) {
                    System.err.println("无法读取文件: " + path);
                    e.printStackTrace();
                }
            });
        return new InnerBencodeFileReader(torrentFileContents, fastResumeFileContents);
    }
}
