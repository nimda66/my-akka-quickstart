package com.lightbend.akka.sample;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import com.lightbend.akka.sample.Printer.Greeting;

//#greeter-messages
public class Greeter extends AbstractActor {
    private final String message;
    private final ActorRef printerActor;
    private String greeting = "";
    //#greeter-messages

    public Greeter(String message, ActorRef printerActor) {
        this.message = message;
        this.printerActor = printerActor;
    }

    //#greeter-messages
    static public Props props(String message, ActorRef printerActor) {
        return Props.create(Greeter.class, () -> new Greeter("Greeter: " + message, printerActor));
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(WhoToGreet.class, whoToGreet -> greeting = message + ", " + whoToGreet.who)
                .match(Greet.class, greet -> {
                    //#greeter-send-message
                    printerActor.tell(new Greeting(greeting), getSelf());
                    //#greeter-send-message
                })
                .build();
    }

    //#greeter-messages
    static public class WhoToGreet {
        public final String who;

        public WhoToGreet(String who) {
            this.who = who;
        }
    }

    static public class Greet {
        public Greet() {
        }
    }
//#greeter-messages
}
//#greeter-messages
