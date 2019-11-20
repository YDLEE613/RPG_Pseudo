public class Criteria {
    private String field;
    private String condition;

    public Criteria(String field, String condition){
        this.field = field;
        this.condition = condition;
    }

    public String getField() {
        return field;
    }

    public String getCondition() {
        return condition;
    }
}
