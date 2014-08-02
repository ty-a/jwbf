package net.sourceforge.jwbf.core.bots;

import com.google.common.collect.ImmutableList;
import net.sourceforge.jwbf.core.contentRep.SimpleArticle;
import net.sourceforge.jwbf.core.contentRep.Userinfo;

/**
 * Main interface for all kinds of wikibots.
 *
 * @author Thomas Stock
 */
public interface WikiBot {

  /**
   * @deprecated use {@link #readData(int, String)}
   */
  SimpleArticle readData(final String name, final int properties);

  SimpleArticle readData(final int properties, final String name);

  ImmutableList<SimpleArticle> readData(final int properties, final String... names);

  ImmutableList<SimpleArticle> readData(final String... names);

  SimpleArticle readData(final String name);

  ImmutableList<SimpleArticle> readData(final int properties, final ImmutableList<String> names);

  ImmutableList<SimpleArticle> readData(final ImmutableList<String> names);

  void writeContent(SimpleArticle sa);

  void delete(String title);

  void login(String user, String passwd);

  Userinfo getUserinfo();

  String getWikiType();

}
