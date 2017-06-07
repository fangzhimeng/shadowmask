/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.shadowmask.core.domain;

import org.junit.Assert;
import org.junit.Test;
import org.shadowmask.core.domain.tree.ComparableDomainTree.ComparableDomainTreeNode;
import org.shadowmask.core.domain.tree.LongDomainTree;

public class TestNumbericDomainTree {
  @Test public void testTree() {
    LongDomainTree tree = new LongDomainTree();
    tree.constructFromYamlInputStream(this.getClass().getClassLoader()
        .getResourceAsStream("Interval-Mask.yaml"));

    boolean hasRevert = false;
    for (int i = 0; i < tree.getLeaves().size() - 1; i++) {
      if (tree.getLeaves().get(i).getlBound() > tree.getLeaves().get(i + 1)
          .getlBound()) {
        hasRevert = true;
      }
    }
    Assert.assertTrue(!hasRevert);
    ComparableDomainTreeNode<Long> node = tree.fixALeaf(30L);
    System.out.println();
  }
}
