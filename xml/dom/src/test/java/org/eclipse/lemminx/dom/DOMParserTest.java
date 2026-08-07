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
package org.eclipse.lemminx.dom;

import static com.google.common.truth.Truth.assertThat;

import org.eclipse.lemminx.uriresolver.URIResolverExtensionManager;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * Tests for {@link DOMParser}, the tolerant XML parser backing the XML editor, completion,
 * diagnostics and every other XML language feature. It must keep producing a usable DOM even for
 * malformed/unclosed markup, since it parses documents as the user is actively typing them.
 */
class DOMParserTest {

  private static DOMDocument parse(String xml) {
    return DOMParser.getInstance().parse(xml, "test.xml", new URIResolverExtensionManager());
  }

  @Test
  void parsesSimpleElementTree() {
    DOMDocument document = parse("<root><child/></root>");

    DOMElement root = document.getDocumentElement();
    assertThat(root.getTagName()).isEqualTo("root");
    assertThat(root.getChildren()).hasSize(1);

    DOMElement child = (DOMElement) root.getChild(0);
    assertThat(child.getTagName()).isEqualTo("child");
    assertThat(child.isSelfClosed()).isTrue();
  }

  @Test
  void parsesAttributes() {
    DOMDocument document = parse("<root id=\"1\" name='foo'/>");

    DOMElement root = document.getDocumentElement();
    assertThat(root.getAttribute("id")).isEqualTo("1");
    assertThat(root.getAttribute("name")).isEqualTo("foo");
    assertThat(root.hasAttribute("missing")).isFalse();
  }

  @Test
  void parsesTextContent() {
    DOMDocument document = parse("<root>hello world</root>");

    DOMElement root = document.getDocumentElement();
    DOMNode textNode = root.getChild(0);
    assertThat(textNode.isText()).isTrue();
    assertThat(((DOMText) textNode).getData()).isEqualTo("hello world");
  }

  @Test
  void parsesComments() {
    DOMDocument document = parse("<root><!-- a comment --></root>");

    DOMNode comment = document.getDocumentElement().getChild(0);
    assertThat(comment.isComment()).isTrue();
    assertThat(((DOMComment) comment).getData()).isEqualTo(" a comment ");
  }

  @Test
  void parsesCData() {
    DOMDocument document = parse("<root><![CDATA[<not-a-tag>]]></root>");

    DOMNode cdata = document.getDocumentElement().getChild(0);
    assertThat(cdata.isCDATA()).isTrue();
    assertThat(((DOMText) cdata).getData()).isEqualTo("<not-a-tag>");
  }

  @Test
  void parsesNestedElements() {
    DOMDocument document = parse("<a><b><c/></b><d/></a>");

    DOMElement a = document.getDocumentElement();
    assertThat(a.getChildren()).hasSize(2);

    DOMElement b = (DOMElement) a.getChild(0);
    assertThat(b.getTagName()).isEqualTo("b");
    assertThat(b.getChildren()).hasSize(1);
    assertThat(((DOMElement) b.getChild(0)).getTagName()).isEqualTo("c");

    assertThat(((DOMElement) a.getChild(1)).getTagName()).isEqualTo("d");
  }

  @Test
  void toleratesUnclosedTag() {
    // The parser must not throw on malformed input; it runs on every keystroke.
    DOMDocument document = parse("<root><child></root>");

    DOMElement root = document.getDocumentElement();
    assertThat(root.getTagName()).isEqualTo("root");
    List<DOMNode> children = root.getChildren();
    assertThat(children).isNotEmpty();
    assertThat(((DOMElement) children.get(0)).getTagName()).isEqualTo("child");
  }

  @Test
  void toleratesUnclosedAttributeValue() {
    // Should not throw while the user is still typing the closing quote.
    DOMDocument document = parse("<root attr=\"value>");

    assertThat(document.getDocumentElement().getTagName()).isEqualTo("root");
  }

  @Test
  void parsesEmptyDocument() {
    DOMDocument document = parse("");

    assertThat(document.getDocumentElement()).isNull();
  }

  @Test
  void findNodeAt_returnsInnermostElementContainingOffset() {
    DOMDocument document = parse("<a><b>text</b></a>");

    // offset inside <b>text</b>
    int offsetInsideB = "<a><b>te".length();
    DOMNode found = document.findNodeAt(offsetInsideB);

    assertThat(found.isText()).isTrue();
    assertThat(found.getParentNode()).isInstanceOf(DOMElement.class);
    assertThat(((DOMElement) found.getParentNode()).getTagName()).isEqualTo("b");
  }
}
