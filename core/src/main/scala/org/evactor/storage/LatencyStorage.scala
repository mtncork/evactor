/*
 * Copyright 2012 Albert Örwall
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.evactor.storage

import org.evactor.model.events.Event
import org.evactor.model.State

@deprecated("latency should be calculated with a processor instead", "0.2")
trait LatencyStorage {

  def getLatencyStatistics(channel: String, fromTimestamp: Option[Long], toTimestamp: Option[Long], interval: String): (Long, List[(Long, Long)])
  
}
