package pm.feel.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ValueUpdate {
    String mac;
    Double vA, vB, vC;
}
