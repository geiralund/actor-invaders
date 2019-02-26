package space.invaders.gamestate;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import space.invaders.dto.AlienDto;
import space.invaders.dto.BulletDto;
import space.invaders.dto.GameStateDto;
import space.invaders.dto.PlayerDto;

import java.util.ArrayList;
import java.util.List;

public class Game extends AbstractActor {
    private final ActorRef guiActor;
    private final int width = GameStateDto.screenSize.width;
    private final int height = GameStateDto.screenSize.height;
    private ActorRef player;
    private PlayerDto playerDto;
    private List<BulletDto> bullets = new ArrayList<>();
    private List<AlienDto> aliens = new ArrayList<>();

    private LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    static public Props props(ActorRef guiActor) {
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


    private Receive getIdle() {
        return receiveBuilder()
                .match(Start.class, start -> {
                        this.player = getContext().actorOf(Player.props(), "player");
                        log.info("Game started!");
                        getContext().become(getPlaying());
                })
                .build();
    }

    private Receive getPlaying() {
        return receiveBuilder()
                .match(Tick.class, tick -> guiActor.tell(new GameStateDto(GameStateDto.State.Playing, playerDto, bullets, aliens), getSelf()))
                .match(MoveLeft.class, ml -> player.tell(ml, getSelf()))
                .match(MoveRight.class, mr -> player.tell(mr, getSelf()))
                .match(PlayerDto.class, playerDto -> this.playerDto = playerDto)
                .build();
    }

    @Override
    public Receive createReceive() {
        return getIdle();
    }

}