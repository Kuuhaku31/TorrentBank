package Database;

import BencodeParser.BencodeParser;

public class FastResumeRecode extends DatabaseRecode {

    final String category;
    final byte[] fastResumeFileContent;

    public FastResumeRecode(byte[] fastResumeFileContent) {
        var fastResumeParser = new BencodeParser(fastResumeFileContent);
        super(fastResumeParser.getHashFromTorrent());
        this.category = fastResumeParser.getCategoryFromFastResume();
        this.fastResumeFileContent = fastResumeFileContent;

        if(this.category == null || this.fastResumeFileContent == null) {
            throw new IllegalArgumentException("无效的 fastresume 文件内容，无法提取必要信息");
        }
    }

    @Override
    public String toString() {
        return "FastResumeRecode{" +
            "TOR_HASH='" + TOR_HASH + '\'' +
            ", category='" + category + '\'' +
            ", fastResumeFileContent=" + (fastResumeFileContent != null ? fastResumeFileContent.length + " bytes" : "null") +
        '}';
    }
}
