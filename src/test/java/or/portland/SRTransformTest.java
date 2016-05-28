package or.portland;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import net.sf.saxon.s9api.*;

import java.util.Iterator;

public class SRTransformTest
    extends TestCase
{
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public SRTransformTest(String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( SRTransformTest.class );
    }

    public String callSRT(String x, String y, String sSR, String tSR) {
        String result = "";
        try {
            Processor proc = new Processor(false);
            proc.registerExtensionFunction(new SRTransform());
            XPathCompiler comp = proc.newXPathCompiler();
            comp.declareNamespace("srxform", "java:or.portland.SRTransform");
            comp.declareVariable(new QName("arg1"));
            comp.declareVariable(new QName("arg2"));
            comp.declareVariable(new QName("arg3"));
            comp.declareVariable(new QName("arg4"));
            XPathExecutable exp = comp.compile("srxform:srtransform($arg1, $arg2, $arg3, $arg4)");
            XPathSelector ev = exp.load();
            ev.setVariable(new QName("arg1"), new XdmAtomicValue(x));
            ev.setVariable(new QName("arg2"), new XdmAtomicValue(y));
            ev.setVariable(new QName("arg3"), new XdmAtomicValue(sSR));
            ev.setVariable(new QName("arg4"), new XdmAtomicValue(tSR));
            XdmValue val = ev.evaluate();
            Iterator i = val.iterator();
            while (i.hasNext()) {
                XdmAtomicValue coord = (XdmAtomicValue)i.next();
                System.out.println(coord);
                result += coord + " ";
            }
            System.out.println(result);
            exp = null;
        }
        catch (Throwable t) {
            t.printStackTrace();
            assertTrue(false);
        }
        return result;
    }

    // epsg:2286 - S. Washington state plane, epsg: 2913 - N. Oregon state plane
    public void testSuccess() {
        String result = callSRT("1127734", "129164", "epsg:2286", "epsg:2913");
        assertTrue("7689468 736607 ".equals(result));
    }

    public void testAlphaX() {
        String result = callSRT("ABC", "129164", "epsg:2286", "epsg:2913");
        assertTrue("0 129164 ".equals(result));
    }

    public void testAlphaY() {
        String result = callSRT("129164", "ABC", "epsg:2286", "epsg:2913");
        assertTrue("129164 0 ".equals(result));
    }
    public void testBadSR() {
        String result = callSRT("1127734", "129164", "epsg:2286", "blah");
        assertTrue("0 0 ".equals(result));
    }
}
