package space.invaders.gamestate;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import space.invaders.dto.GameStateDto;
import space.invaders.dto.Image;
import space.invaders.dto.PlayerDto;

public class Player extends AbstractActor {
    public static final int STEPS = 5;
    private int lives = 3;
    private int posX;
    private int posY;
    private static final int width = 50;
    private static final int height = 50 * 140/280;
    private static final Image image = new Image(width, height, "img/cannon.png");
    private static final int sceneWidth = GameStateDto.screenSize.width;
    private static final int sceneHeight = GameStateDto.screenSize.height;
    private LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    static Props props() {
        return Props.create(Player.class, Player::new);
    }

    private Player(){
        posX = (int) ((sceneWidth + width) * 0.5);
        posY = (sceneHeight - height);
        getContext().parent().tell(new Update(createPlayer()), getSelf());
    }

    public static class Update {
        public PlayerDto playerDto;

        public Update(PlayerDto playerDto) {
            this.playerDto = playerDto;

        }
    }
    public static class Fire {
        public ActorRef bulletManager;

        public Fire(ActorRef bulletManager) {
            this.bulletManager = bulletManager;
        }
    }

    @Override
    public Receive createReceive() {
        return
                receiveBuilder()
                        .match(Game.MoveLeft.class, moveLeft -> {
                            getContext().parent().tell(new Update(moveLeft()), getSelf());
                            log.info("moveLeft");

                        })
                        .match(Game.MoveRight.class, moveRight -> {
                            log.info("moveRight");
                            getContext().parent().tell(new Update(moveRight()), getSelf());
                        })
                        .match(Fire.class, fire -> {
                            fire.bulletManager.tell(new BulletManager.CreateBullet((int) (posX+width*0.5), posY), getSelf());
                        })
                        .build();
    }

    private PlayerDto createPlayer(){
        return new PlayerDto(posX, posY, lives, image);
    }
    private PlayerDto moveLeft(){
        move(-STEPS);

        return createPlayer();
    }

    private PlayerDto moveRight(){
        move(STEPS);
        return createPlayer();
    }

    private void move(int moving){
        posX = Math.min(sceneWidth  - width, Math.max(posX + moving, 0));
    }
}
