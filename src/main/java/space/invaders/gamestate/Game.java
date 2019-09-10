package space.invaders.gamestate;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import space.invaders.dto.*;

import java.util.ArrayList;
import java.util.Collection;
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
    private ActorRef bulletmanager;
    private ActorRef alienManager;

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
                    bulletmanager = getContext().actorOf(BulletManager.props(), "bulletmanager");
                    alienManager = getContext().actorOf(AlienManager.props(bulletmanager), "alienmanager");
                    getContext().become(playing());
                } )
                .build();
    }


    private Receive playing() {

        return receiveBuilder()
                .match(Tick.class, tick -> {
                    bulletmanager.forward(tick, getContext());
                    alienManager.forward(tick, getContext());
                    updateGui();
                })
                .match(MoveLeft.class, moveLeft -> {
                    player.tell(moveLeft, getSelf());
                    log.info("moveLeft - game");
                })
                .match(MoveRight.class, moveRight -> {
                    player.tell(moveRight, getSelf());
                    log.info("moveRight - game");
                })
                .match(Fire.class, fire -> {
                    player.tell(new Player.Fire(bulletmanager), getSelf());
                    log.info("fire - game");
                })
                .match(BulletManager.Update.class, update -> {
                    bullets = update.bulletDtoList;
                })
                .match(AlienManager.Update.class, update -> {
                    aliens = update.alienDtoList;
                })
                .match(Player.Update.class, update -> {
                    playerDto = update.playerDto;
                })

                .build();

    }

    private void updateGui() {
        guiActor.tell(new GameStateDto(GameStateDto.State.Playing, playerDto, bullets, aliens), getSelf());
    }


}