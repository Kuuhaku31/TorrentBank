package Database;

import BencodeParser.BencodeParser;

public class TorrentRecode extends DatabaseRecode{

    final String file_name;
    final Long   file_size;
    final byte[] torrentFileContent;

    public TorrentRecode(byte[] torrentFileContent) {
        var torrentParser = new BencodeParser(torrentFileContent);
        super(torrentParser.getHashFromTorrent());
        this.file_name = torrentParser.getFileNameFromTorrent();
        this.file_size = torrentParser.getFileSizeFromTorrent();
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
