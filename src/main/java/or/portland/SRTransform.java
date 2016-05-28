package or.portland;

import java.io.InputStream;

import net.sf.saxon.lib.ExtensionFunctionCall;
import net.sf.saxon.lib.ExtensionFunctionDefinition;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.ZeroOrMore;
import net.sf.saxon.value.SequenceType;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.StringValue;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * <p>This class implements an xsl function that can be used to convert
 * xy coordinates from one spatial reference to another via a call to
 * gdaltransform. The form of the xsl function is:</p>
 *
 * <p><code>srxform:srtransform([coord-string-x], [coord-string-y], [source-sr], [target-sr])</code></p>
 *
 *    <ul><li>coord-string-x: The source X coordinate</li>
 *    <li>coord-string-y: The source Y coordinate</li>
 *    <li>source-sr: The spatial reference of the source coordinates as required by the
 *    -s_srs switch of gdaltransform.</li>
 *    <li>target-sr: The spatial reference of the output coordinates as required by the
 *    -t_srs switch of gdaltransform.</li></ul>
 *
 * <p><code>srxform</code> is a namespace name pointing to the namespace <code>java:or.portland.SRTransform</code></p>
 *
 * <p>Coordinates passed in, although passed in as strings, must be integers
 * or decimals.</p>
 *
 * <p>This function relies on the GDAL library. Specifically, GDAL must be
 * present on the server, and an environment variable GDAL_APPS must be set
 * that points to the directory where the gdaltransform executable can be
 * found. If the environment variable is not set or gdaltransform can't be
 * found in the GDAL_APPS directory, the xsl function will not fail, but it
 * also will not perform the transformation.</p>
 *
 * <p>This function will also not fail when passed bad data. If it is passed
 * coordinate values that are not parseable as a number, it will return
 * a 0 for that coordinate. If any exceptions are thrown while transforming
 * the coordinates, both coordinates will be returned as zeroes.</p>
 *
 * @author  Chris Johnk
 */
public class SRTransform extends ExtensionFunctionDefinition {

    final String gdalTransformExec;

    public SRTransform() {
        // get gdal apps directory from environment variable
        Map<String, String> env = System.getenv();
        String gdalDataDir = env.get("GDAL_APPS");

        // get executable file extension for current os: .exe for windows, nothing
        // for other operating systems
        String execExt = "";
        if (System.getProperty("os.name").startsWith("Windows")) {
            execExt = ".exe";
        }

        // set absolute path to transformation executable
        String fs = System.getProperty("file.separator");
        this.gdalTransformExec = gdalDataDir + fs + "gdaltransform" + execExt;
    }

    @Override
    public StructuredQName getFunctionQName() {
        return new StructuredQName("srxform", "java:or.portland.SRTransform", "srtransform");
    }

    @Override
    public SequenceType[] getArgumentTypes() {
        return new SequenceType[]{SequenceType.SINGLE_STRING, SequenceType.SINGLE_STRING, SequenceType.SINGLE_STRING, SequenceType.SINGLE_STRING};
    }

    @Override
    public SequenceType getResultType(SequenceType[] suppliedArgumentTypes) {
        return SequenceType.STRING_SEQUENCE;
    }

    @Override
    public ExtensionFunctionCall makeCallExpression() {
        return new SRTExtension();
    }

    private class SRTExtension extends ExtensionFunctionCall {
        private String numCheck (String coordinate) {
            String result = "0";
            try {
                // check that coordinate can be parsed as a double
                Double.parseDouble(coordinate);
                result = coordinate;
            }
            catch (NumberFormatException nfe) {
                nfe.printStackTrace();
            }
            return result;
        }

        @Override
        public Sequence call(XPathContext context, Sequence[] arguments) throws XPathException {
            // get source values from call
            String srcX = numCheck(arguments[0].iterate().next().getStringValue());
            String srcY = numCheck(arguments[1].iterate().next().getStringValue());
            String srFrom = arguments[2].iterate().next().getStringValue();
            String srTo = arguments[3].iterate().next().getStringValue();

            String targetX = srcX;
            String targetY = srcY;
            String srcXY = srcX + " " + srcY;
            if (!"0".equals(srcX) && !"0".equals(srcY)) {
                try {
                    // set up the call to gdaltransform
                    Runtime r = Runtime.getRuntime();
                    String[] args = {gdalTransformExec, "-s_srs", srFrom, "-t_srs", srTo};
                    Process p = r.exec(args);
                    // get the stream to which we pass the source coordinates for processing
                    OutputStream pIn = p.getOutputStream();
                    pIn.write(srcXY.getBytes());
                    pIn.close();

                    // read the output from the command into a StringBuffer
                    final InputStream pOut = p.getInputStream();
                    final StringBuffer buf = new StringBuffer();
                    // execute the read operation in a thread that we can
                    // wait for to assure that the results are fully read
                    Thread outputDrainer = new Thread() {
                        public void run() {
                            try {
                                int c;
                                do {
                                    c = pOut.read();
                                    if (c >= 0)
                                        buf.append((char) c);
                                }
                                while (c >= 0);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    };
                    outputDrainer.start();
                    p.waitFor();

                    // split out the x and y coordinates and strip the decimal places
                    String[] targetXYArray = buf.toString().trim().split(" ");
                    targetX = targetXYArray[0].split("\\.")[0];
                    targetY = targetXYArray[1].split("\\.")[0];
                } catch (Throwable t) {
                    targetX = "0";
                    targetY = "0";
                    t.printStackTrace();
                }
            }

            List<Item> targetXYList = new ArrayList<Item>();
            // add the coordinates to the return list
            targetXYList.add(StringValue.makeStringValue(targetX));
            targetXYList.add(StringValue.makeStringValue(targetY));
            return new ZeroOrMore<Item>(targetXYList);
        }
    };
}