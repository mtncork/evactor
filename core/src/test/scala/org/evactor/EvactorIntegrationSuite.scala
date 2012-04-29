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
package org.evactor

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.matchers.MustMatchers
import org.scalatest.BeforeAndAfterAll
import org.scalatest.FunSuite
import akka.actor.actorRef2Scala
import akka.actor.ActorSystem
import akka.actor.Props
import akka.testkit.CallingThreadDispatcher
import akka.testkit.TestKit
import akka.testkit.TestProbe
import akka.util.duration.intToDurationInt
import collect.Collector
import org.evactor.model.events.AlertEvent
import org.evactor.model.events.LogEvent
import org.evactor.model.Start
import org.evactor.model.Success
import org.evactor.process.analyse.latency.Latency
import org.evactor.process.analyse.window.LengthWindowConf
import org.evactor.process.build.request.Request
import org.evactor.process.build.simpleprocess.SimpleProcess
import org.evactor.process.ProcessorEventBusExtension
import org.evactor.process.ProcessorHandler
import org.evactor.process.Subscription
import org.evactor.storage.EventStorageSpec
import org.evactor.process.Subscriptions
import org.evactor.model.Message
import org.evactor.process.StaticPublication
import org.evactor.process.TestPublication

/**
 * Testing the whole log data flow.
 *
 */
@RunWith(classOf[JUnitRunner])
class EvactorIntegrationSuite(_system: ActorSystem) 
	extends TestKit(_system) with FunSuite with MustMatchers with BeforeAndAfterAll {
  
  def this() = this(ActorSystem("EvactorIntegrationSuite", EventStorageSpec.storageConf))

  override protected def afterAll(): scala.Unit = {
    system.shutdown()
  }

  test("Recieve log events and send an alert") {    
    
  	val probe = TestProbe()
  	 
    var result: AlertEvent = null
    val processId = "processId"
    val camelEndpoint = "hej"

    // Start up the modules
    val collector = system.actorOf(Props[Collector].withDispatcher(CallingThreadDispatcher.Id), name = "collect")
    val processor = system.actorOf(Props[ProcessorHandler].withDispatcher(CallingThreadDispatcher.Id), name = "process")
          
    // start the processors
    val reqSubscriptions = Subscriptions("request")  

    processor ! new Request("startComponent", List(new Subscription(Some("startComponent"), None)), new StaticPublication("request", None), 120000L)
  	processor ! new Request("endComponent", List(new Subscription(Some("endComponent"), None)), new StaticPublication("request", None), 120000L)
    processor ! new SimpleProcess("simpleProcess", reqSubscriptions, new StaticPublication(processId, None), List("startComponent", "endComponent"), 120000l)  
    processor ! new Latency("latency", List(new Subscription(Some(processId), None)), new TestPublication(probe.ref), 2000, Some(new LengthWindowConf(2)))

    ProcessorEventBusExtension(system).subscribe(probe.ref, new Subscription(Some("latency"), None))
        
    // Collect logs
    val currentTime = System.currentTimeMillis

    Thread.sleep(400)

    collector ! new Message("startComponent", None, new LogEvent("329380921309", currentTime, "329380921309", "startComponent", "client", "server", Start, "hello"))
    collector ! new Message("startComponent", None, new LogEvent("329380921310", currentTime+1000, "329380921309", "startComponent", "client", "server" , Success, "")) // success
    collector ! new Message("endComponent", None, new LogEvent("329380921311", currentTime+2000, "329380921309", "endComponent", "client", "server", Start, ""))
    collector ! new Message("endComponent", None, new LogEvent("329380921312",  currentTime+3000, "329380921309", "endComponent", "client", "server", Success, "")) // success

    Thread.sleep(400)
    
    probe.expectMsgAllClassOf(1 seconds, classOf[AlertEvent]) // the latency alert
  }
}