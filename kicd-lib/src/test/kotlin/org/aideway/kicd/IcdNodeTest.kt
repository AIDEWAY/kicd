package org.aideway.kicd

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class IcdNodeTest {
    @Test
    fun testFindNode() {
        val root = TestHelp.db.root

        var code = root.findNode("B91", IcdNodeType.CODE)
        assertNotNull(code)
    }

    @Test
    fun testFindNode_exactMatch() {
        val root = TestHelp.db.root
        val node = root.findNode("B91", IcdNodeType.CODE)
        assertNotNull(node)
        assertEquals("B91", node.name)
    }

    @Test
    fun testFindNode_wrongType() {
        val root = TestHelp.db.root
        val node = root.findNode("B91", IcdNodeType.CHAPTER)
        assertNull(node, "Finding a CODE name with CHAPTER type should return null")
    }

    @Test
    fun testBillableCodesAreLeafNodes() {
        val root = TestHelp.db.root
        val billable = root.billableCodes()
        assertTrue(billable.isNotEmpty(), "Should have billable codes")
        for (code in billable) {
            assertTrue(code.children.isEmpty(),
                "Billable code ${code.name} should have no children")
        }
    }

    @Test
    fun testNonBillableCodesHaveChildren() {
        val root = TestHelp.db.root
        val allCodes = root.flatten().filterIsInstance<Code>()
        val nonBillable = allCodes.filter { !it.billable }
        for (code in nonBillable) {
            assertTrue(code.children.isNotEmpty(),
                "Non-billable code ${code.name} should have children")
        }
    }

    @Test
    fun testFlatten_includesSelf() {
        val root = TestHelp.db.root
        val flat = root.flatten()
        assertTrue(flat.contains(root), "flatten() should include the node itself")
    }

    @Test
    fun testChapter_childrenAreSections() {
        val root = TestHelp.db.root
        assertTrue(root.children.isNotEmpty(), "Root should have chapters")
        val chapter = root.children[0]
        for (child in chapter.children) {
            assertTrue(child is Section,
                "Chapter children should be Sections, got ${child.javaClass.simpleName}")
        }
    }

    @Test
    fun testCompareTo_sameType() {
        val root = TestHelp.db.root
        val chapters = root.children
        if (chapters.size >= 2) {
            val cmp = chapters[0].compareTo(chapters[1])
            assertEquals(chapters[0].name.compareTo(chapters[1].name), cmp,
                "Same-type comparison should be by name")
        }
    }

    @Test
    fun testCompareTo_differentType() {
        val root = TestHelp.db.root
        val chapter = root.children[0]
        val cmp = root.compareTo(chapter)
        assertTrue(cmp < 0, "ROOT should come before CHAPTER")
    }


    @Test
    fun testFindNode_longNonExistentCode() {
        val root = TestHelp.db.root
        assertNull(root.findNode("ZZZ99", IcdNodeType.CODE))
    }

    @Test
    fun testFindNode_nestedCode() {
        val root = TestHelp.db.root
        val node = root.findNode("D05.11", IcdNodeType.CODE)
        assertNotNull(node, "Should find nested code D05.11 (child of D05.1, grandchild of D05)")
        assertEquals("D05.11", node.name)
    }
}
