/*
 * Copyright 2007 Thomas Stock.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 * Contributors:
 *
 */
package net.sourceforge.jwbf.mediawiki.actions.util;

import java.util.List;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.primitives.Ints;
import net.sourceforge.jwbf.core.actions.ContentProcessable;
import net.sourceforge.jwbf.core.actions.util.HttpAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Thomas Stock
 */
public abstract class MWAction implements ContentProcessable {

  private static final Logger log = LoggerFactory.getLogger(MWAction.class);

  /**
   * @deprecated remove
   */
  @Deprecated
  private boolean hasMore = true;
  static final ExceptionHandler DEFAULT_EXCEPTION_HANDLER = new ExceptionHandler() {

    @Override
    public void handle(RuntimeException e) {
      throw e;

    }
  };

  private static ExceptionHandler exceptionHandler = DEFAULT_EXCEPTION_HANDLER;

  /**
   * @return true if and changes state to false
   */
  @Override
  public boolean hasMoreMessages() {
    final boolean b = hasMore;
    hasMore = false;
    return b;
  }

  /**
   * @deprecated do not use this method TODO rm
   */
  @Deprecated
  public void setHasMoreMessages(boolean b) {
    hasMore = b;
  }

  protected MWAction() {

  }

  /**
   * Deals with the MediaWiki API's response by parsing the provided text.
   *
   * @param s  the answer to the most recently generated MediaWiki API request
   * @param hm the requestor message
   * @return the returning text on processing problems
   */
  @Override
  public String processReturningText(final String s, final HttpAction hm) {
    return processAllReturningText(s);
  }

  /**
   * @param s the returning text
   * @return the returning text never
   */
  public String processAllReturningText(final String s) {
    return s;
  }

  /**
   * helper method generating a namespace string as required by the MW-api.
   *
   * @param namespaces namespace as
   * @return with numbers seperated by |
   */
  public static String createNsString(int... namespaces) {
    return createNsString(nullSafeCopyOf(namespaces));
  }

  public static ImmutableList<Integer> nullSafeCopyOf(int[] namespaces) {
    if (namespaces == null) {
      return ImmutableList.of();
    } else {
      return ImmutableList.copyOf(Ints.asList(namespaces));
    }
  }

  public static String createNsString(List<Integer> asList) {
    return Joiner.on("|").join(asList);
  }

  /**
   * {@inheritDoc}
   *
   * @deprecated see interface
   */
  @Override
  @Deprecated
  public boolean isSelfExecuter() {
    return false;
  }

  /**
   * @deprecated no alternative method is given, because it is not recommended to change the
   * exception handling
   */
  @Deprecated
  public static void setExceptionHandler(ExceptionHandler exceptionHandler) {
    log.warn("it is not recommended, to change the default handler, because of API changes");
    MWAction.exceptionHandler = exceptionHandler;
  }

}
