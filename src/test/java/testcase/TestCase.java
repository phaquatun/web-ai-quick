

package testcase;

import io.vertx.core.json.JsonObject;



public class TestCase {
    
    public static void main(String[] args) {
        JsonObject json = new JsonObject();
        json.put("id", "đisdsd").put("pass", "sjdks");
        
        json.remove("id");
        System.out.println(json);
    }
    
}
