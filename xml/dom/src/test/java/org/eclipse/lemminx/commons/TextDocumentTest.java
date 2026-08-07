/*
 *  This file is part of AndroidIDE.
 *
 *  AndroidIDE is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  AndroidIDE is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *   along with AndroidIDE.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.eclipse.lemminx.commons;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.tom.rv2ide.models.Position;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link TextDocument}, which backs every offset/line/column translation used by the
 * XML DOM parser and language services (diagnostics ranges, hover positions, completion, etc.).
 */
class TextDocumentTest {

  @Test
  void positionAt_onSingleLineText_returnsLineZero() throws BadLocationException {
    TextDocument doc = new TextDocument("hello", "test.xml");

    assertThat(doc.positionAt(0)).isEqualTo(new Position(0, 0));
    assertThat(doc.positionAt(5)).isEqualTo(new Position(0, 5));
  }

  @Test
  void positionAt_afterLineFeed_advancesToNextLine() throws BadLocationException {
    TextDocument doc = new TextDocument("ab\ncd", "test.xml");

    assertThat(doc.positionAt(0)).isEqualTo(new Position(0, 0));
    assertThat(doc.positionAt(2)).isEqualTo(new Position(0, 2));
    // offset 3 is right after the '\n', i.e. start of the second line
    assertThat(doc.positionAt(3)).isEqualTo(new Position(1, 0));
    assertThat(doc.positionAt(5)).isEqualTo(new Position(1, 2));
  }

  @Test
  void positionAt_withCrlfDelimiters_treatsCrlfAsSingleDelimiter() throws BadLocationException {
    TextDocument doc = new TextDocument("ab\r\ncd", "test.xml");

    assertThat(doc.positionAt(4)).isEqualTo(new Position(1, 0));
    assertThat(doc.positionAt(6)).isEqualTo(new Position(1, 2));
  }

  @Test
  void offsetAt_isInverseOfPositionAt() throws BadLocationException {
    TextDocument doc = new TextDocument("first\nsecond\nthird", "test.xml");

    for (int offset = 0; offset <= doc.getText().length(); offset++) {
      Position position = doc.positionAt(offset);
      assertThat(doc.offsetAt(position)).isEqualTo(offset);
    }
  }

  @Test
  void positionAt_negativeOffset_throwsBadLocationException() {
    TextDocument doc = new TextDocument("abc", "test.xml");

    assertThrows(BadLocationException.class, () -> doc.positionAt(-1));
  }

  @Test
  void positionAt_offsetPastEndOfText_throwsBadLocationException() {
    TextDocument doc = new TextDocument("abc", "test.xml");

    assertThrows(BadLocationException.class, () -> doc.positionAt(4));
  }

  @Test
  void lineText_excludesLineDelimiter() throws BadLocationException {
    TextDocument doc = new TextDocument("first\nsecond\nthird", "test.xml");

    assertThat(doc.lineText(0)).isEqualTo("first");
    assertThat(doc.lineText(1)).isEqualTo("second");
    assertThat(doc.lineText(2)).isEqualTo("third");
  }

  @Test
  void lineDelimiter_reflectsDelimiterUsedOnThatLine() throws BadLocationException {
    TextDocument doc = new TextDocument("a\nb\r\nc", "test.xml");

    assertThat(doc.lineDelimiter(0)).isEqualTo("\n");
    assertThat(doc.lineDelimiter(1)).isEqualTo("\r\n");
  }

  @Test
  void emptyDocument_hasSingleEmptyLine() throws BadLocationException {
    TextDocument doc = new TextDocument("", "test.xml");

    assertThat(doc.positionAt(0)).isEqualTo(new Position(0, 0));
  }

  @Test
  void positionAt_onTrailingNewline_pointsToStartOfNewEmptyLine() throws BadLocationException {
    TextDocument doc = new TextDocument("abc\n", "test.xml");

    assertThat(doc.positionAt(4)).isEqualTo(new Position(1, 0));
  }
}
