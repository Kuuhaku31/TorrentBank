package Database;


abstract public class DatabaseRecode {

    final String TOR_HASH;

    public DatabaseRecode(String TOR_HASH) {
        this.TOR_HASH = TOR_HASH; 

        if(this.TOR_HASH == null || this.TOR_HASH.isEmpty()) {
            throw new IllegalArgumentException("TOR_HASH 不能为空");
        }
    }

    @Override
    public String toString() {return "DatabaseRecode{" +"TOR_HASH='" + TOR_HASH + '\'' +'}';}
}
