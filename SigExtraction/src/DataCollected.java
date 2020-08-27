

public class DataCollected {
    private String tel;
    private String job;
    
    public DataCollected() {
        this.tel = "";
        this.job = "";
    }

    public DataCollected(String tel, String job) {
        this.tel = tel;
        this.job = job;
    }

    public String getTel() {
        return tel;
    }

    public String getJob() {
        return job;
    }
    
    public void setTel(String tel) {
        this.tel = tel;
    }

    public void setJob(String job) {
        this.job = job;
    }
}
