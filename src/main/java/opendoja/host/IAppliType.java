package opendoja.host;

import java.util.Properties;

public enum IAppliType {
    I_APPLI,
    I_APPLI_DX;

    static IAppliType fromJamProperties(Properties properties) {
        if (properties == null) {
            return I_APPLI;
        }
        return properties.containsKey("TrustedAPID") ? I_APPLI_DX : I_APPLI;
    }
}
