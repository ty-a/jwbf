package net.sourceforge.jwbf.actions.http.mw;

import static org.junit.Assert.*;

import java.util.Vector;

import net.sourceforge.jwbf.FileLoader;

import org.htmlparser.Node;
import org.junit.Before;
import org.junit.Test;

public class GetCategoryArticlesTest {

	private GetCategoryArticles ga;
	
	@Before
	public void setUp() throws Exception {
		ga = new GetCategoryArticles("", new Vector<String>());
	}

	@Test
	public final void testParseHasMore() {
		Node text1 = getText(ga, FileLoader.MW1_10
				+ "category_Weiblicher_Vorname_wp1_10_de.htm");
		ga.parseHasMore(text1);
		assertEquals(true, ga.hasMoreElements());
		
	}
	/**
	 * Test if next category is .
	 *
	 */
	@Test
	public final void testGetNextPageId() {
		Node text1 = getText(ga, FileLoader.MW1_10
				+ "category_Weiblicher_Vorname_wp1_10_de.htm");
		ga.parseHasMore(text1);
		assertEquals("Evelyn", ga.nextElement());
	}
	/**
	 * Test if no more pages are found.
	 *
	 */
	@Test
	public final void testGetNextPageId2No() {
		Node text1 = getText(ga, FileLoader.MW1_10
				+ "category_Pigenavne_wp1_10_dk.htm");
		ga.parseHasMore(text1);
		assertEquals("", ga.nextElement());
	}
	
	/**
	 * Test if no more pages are found.
	 *
	 */
	@Test
	public final void testParseHasMoreNo() {
		Node text1 = getText(ga, FileLoader.MW1_10
				+ "category_Pigenavne_wp1_10_dk.htm");
		ga.parseHasMore(text1);
		assertEquals(false, ga.hasMoreElements());
	}
	
	private static Node getText(GetCategoryArticles gl, String file) {
		return gl.getDivBodyContent(FileLoader.readFromFile(file));
	}
}