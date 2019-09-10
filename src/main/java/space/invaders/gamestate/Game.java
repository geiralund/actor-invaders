package space.invaders.gamestate;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import space.invaders.dto.*;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.emptyList;

public class Game extends AbstractActor {
    private final ActorRef guiActor;
    private final int width = GameStateDto.screenSize.width;
    private final int height = GameStateDto.screenSize.height;
    private ActorRef player;
    private PlayerDto playerDto;
    private List<BulletDto> bullets = new ArrayList<>();
    private List<AlienDto> aliens = new ArrayList<>();

    private LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    public static Props props(ActorRef guiActor) {
        return Props.create(Game.class, () -> new Game(guiActor));
    }

    public static class Tick {

    }

    public static class Start {

    }

    public static class Fire {

    }

    public static class MoveLeft {

    }

    public static class MoveRight {

    }

    private Game(ActorRef guiActor) {
        this.guiActor = guiActor;
    }



    @Override
    public Receive createReceive() {
        return  idle();


    }

    public ActorRef createPlayer() {
        return getContext().actorOf(Player.props(), "player");

    }

    private Receive idle() {
        return receiveBuilder()
                .match(Start.class, start ->  {
                    log.info("Started")  ;
                    this.player = createPlayer();
                    getContext().become(playing());
                } )
                .build();
    }


    private Receive playing() {

        return receiveBuilder()
                .match(Tick.class, tick -> {
                    guiActor.tell(new GameStateDto(GameStateDto.State.Playing, null, emptyList(), emptyList()), getSelf());
                })
                .match(MoveLeft.class, moveLeft -> {
                    player.tell(moveLeft, getSelf());
                    log.info("moveLeft - game");
                })
                .match(MoveRight.class, moveRight -> {
                    player.tell(moveRight, getSelf());
                    log.info("moveRight - game");
                })
                .match(Player.Update.class, update -> {
                    this.playerDto = update.playerDto;
                    guiActor.tell(new GameStateDto(GameStateDto.State.Playing, this.playerDto, emptyList(), emptyList()), getSelf());
                })

                .build();

    }


}