package BencodeParser;

public class TestBencodeParser {
    public static void main(String[] args) {

        System.out.println("Hello, BencodeParser!");

        var filePtah = "./ignore/2d0ce0549d27666e1078a86653367675224f02c2.fastresume";
        // var filePtah2 = "./ignore/2d0ce0549d27666e1078a86653367675224f02c2.torrent";

        try {
            var fileContent = java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(filePtah));
            var parser = new BencodeParser(fileContent);
            String hash = parser.getHashFromTorrent();
            String fileName = parser.getFileNameFromTorrent();
            String category = parser.getCategoryFromFastResume();

            System.out.println("Extracted Hash: " + hash);
            System.out.println("Extracted File Name: " + fileName);
            System.out.println("Extracted Category: " + category);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}