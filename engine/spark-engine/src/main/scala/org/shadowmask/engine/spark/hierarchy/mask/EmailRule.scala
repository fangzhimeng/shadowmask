/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.shadowmask.engine.spark.hierarchy.mask

import org.apache.commons.lang3.StringUtils
import scala.util.matching.Regex

class EmailRule(
               hierarchy: Int,
               maskChar: Char) extends GeneralizerRule[String, String] {

  override def mask(input: String): String = {
""


//  def flagValidate(flag:Boolean):Boolean={
//    var  flag=false
//    val locationOfAt = input.indexOf('@');
//    // there is only one '@' in the value and not in the beginning
//    if (locationOfAt > 0 && locationOfAt == input.lastIndexOf('@')) {
//      val locationOfDot = input.lastIndexOf('.');
//      // there is a '.' after '@'
//      if (locationOfDot > locationOfAt) {
//        flag=true
//      }
//    }
//  }




    if (input.length <= hierarchy) {
      StringUtils.repeat(maskChar, input.length)
    } else {

      (hierarchy) match {
        case (0) =>
          input
        case (1) =>
          maskChar + "@" + input.substring(input.indexOf("@") + 1, input.length)
        case (2) =>
          maskChar + "@" + maskChar + "." + input.substring(input.indexOf(".") + 1, input.length)
        case (3) =>
          maskChar + "@" + maskChar + "." + maskChar
      }
    }



  }

}
