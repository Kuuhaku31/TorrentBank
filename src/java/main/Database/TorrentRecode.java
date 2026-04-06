package Database;

import BencodeParser.BencodeParser;

public class TorrentRecode extends DatabaseRecode{

    final String file_name;
    final Long   file_size;
    final byte[] torrentFileContent;

    private record ParsedTorrent(String hash, String fileName, Long fileSize) {}

    private static ParsedTorrent parse(byte[] torrentFileContent) {
        var torrentParser = new BencodeParser(torrentFileContent);
        return new ParsedTorrent(
            torrentParser.getHashFromTorrent(),
            torrentParser.getFileNameFromTorrent(),
            torrentParser.getFileSizeFromTorrent()
        );
    }

    public TorrentRecode(byte[] torrentFileContent) {
        this(parse(torrentFileContent), torrentFileContent);
    }

    private TorrentRecode(ParsedTorrent parsed, byte[] torrentFileContent) {
        super(parsed.hash());
        this.file_name = parsed.fileName();
        this.file_size = parsed.fileSize();
        this.torrentFileContent = torrentFileContent;

        if(this.file_name == null || this.file_size == null || this.torrentFileContent == null) {
            throw new IllegalArgumentException("无效的 torrent 文件内容，无法提取必要信息");
        }
    }

    @Override
    public String toString() {
        return "TorrentRecode{" +
            "TOR_HASH='" + TOR_HASH + '\'' +
            ", file_name='" + file_name + '\'' +
            ", file_size=" + file_size +
            ", torrentFileContent=" + (torrentFileContent != null ? torrentFileContent.length + " bytes" : "null") +
        '}';
    }
}
