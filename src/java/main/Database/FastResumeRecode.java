package Database;

import BencodeParser.BencodeParser;

public class FastResumeRecode extends DatabaseRecode {

    final String category;
    final String save_path;
    final byte[] fastResumeFileContent;

    private record ParsedFastResume(String hash, String category, String savePath) {}

    private static ParsedFastResume parse(byte[] fastResumeFileContent) {
        var fastResumeParser = new BencodeParser(fastResumeFileContent);
        return new ParsedFastResume(
            fastResumeParser.getHashFromTorrent(),
            fastResumeParser.getCategoryFromFastResume(),
            fastResumeParser.getSavePathFromFastResume()
        );
    }

    public FastResumeRecode(byte[] fastResumeFileContent) {
        this(parse(fastResumeFileContent), fastResumeFileContent);
    }

    private FastResumeRecode(ParsedFastResume parsed, byte[] fastResumeFileContent) {
        super(parsed.hash());
        this.category = parsed.category();
        this.save_path = parsed.savePath();
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
            ", save_path='" + save_path + '\'' +
            ", fastResumeFileContent=" + (fastResumeFileContent != null ? fastResumeFileContent.length + " bytes" : "null") +
        '}';
    }
}
