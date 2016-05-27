package or.portland;

import net.sf.saxon.lib.Initializer;
import net.sf.saxon.Configuration;

/**
 * Created by pbcjohnk on 4/27/2016.
 */
public class SRTInitializer implements Initializer {
    public void initialize(Configuration config) {
        config.registerExtensionFunction(new SRTransform());
        config.setConfigurationProperty("allowExternalFunctions", "true");
    }
}
