package es.ubu.lsi;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Unit test for simple App.
 */
public class ChatTest extends TestCase
{
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public ChatTest( String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( ChatTest.class );
    }

    /**
     * Rigourous Test :-)
     */
    public void testChat()
    {
        assertTrue( true );
    }
}
