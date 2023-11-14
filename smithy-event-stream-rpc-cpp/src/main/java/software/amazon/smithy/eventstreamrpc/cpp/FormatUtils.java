/**
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package software.amazon.smithy.eventstreamrpc.cpp;

import java.math.BigDecimal;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;

public final class FormatUtils {
    public static final int tabWidth = 4;
    public static final String spacedTab = new String(new char[tabWidth]).replace("\0", " ");

    private FormatUtils() {
    }

    /**
     * Takes a string and inserts a fixed number of tabs on each line
     *
     * @param string str input string to tab
     * @return int numTabs number of tabs to insert after each newline
     */
    public static String tabEachLine(String str, int numTabs) {
                return str.replaceAll("\\n", "\n" + new String(new char[numTabs]).replace("\0", spacedTab));
    }
}
