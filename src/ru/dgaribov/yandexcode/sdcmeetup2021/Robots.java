package ru.dgaribov.yandexcode.sdcmeetup2021;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author David Garibov
 */
public class Robots {

    private final static int MAX_ROBOTS = 100;
    private final static int STEPS_PER_INTERACTION = 60;

    private List<Order> orders;
    private List<Robot> robots;
    private boolean[][] cityMap;

    int[] ordersPerIterations;

    public static void main(String[] args) {
        Robots app = new Robots();
        Scanner scanner = new Scanner(System.in);
        // N, MaxTips, Cost
        String[] line = scanner.nextLine().split(" ");
        int N = Integer.parseInt(line[0]);
        int maxTips = Integer.parseInt(line[1]);
        long cost = Long.parseLong(line[2]);
        // true - cell is taken, false - free
        boolean[][] cityMap = new boolean[N][N];
        for (int i = 0; i < N; i++) {
            String ln = scanner.nextLine();
            for (int j = 0; j < N; j++) {
                cityMap[i][j] = '#' == ln.charAt(j);
            }
        }
        app.cityMap = cityMap;

        line = scanner.nextLine().split(" ");

        // Number of iterations
        int T = Integer.parseInt(line[0]);
        app.ordersPerIterations = new int[T];
        // Number of orders
        int D = Integer.parseInt(line[1]);
        app.orders = new ArrayList<>(D);

        int[][] robotCoords = app.robotInitCoordinates(cityMap, maxTips, cost, T, D);
        System.out.println(robotCoords.length);
        app.robots = new ArrayList<>(robotCoords.length);
        for (int[] coords : robotCoords) {
            app.robots.add(new Robot(coords[0], coords[1]));
            System.out.println(coords[0] + " " + coords[1]);
        }

        // Начинаем итерации
        for (int i = 0; i < T; i++) {
            String ln = scanner.nextLine();
            // Количество курьерских заказов
            int k = Integer.parseInt(ln);
            app.ordersPerIterations[i] = k;
            if (k == 0) continue;
            for (int j = 0; j < k; j++) {
                line = scanner.nextLine().split(" ");
                app.orders.add(new Order(
                        Integer.parseInt(line[0]),
                        Integer.parseInt(line[1]),
                        Integer.parseInt(line[2]),
                        Integer.parseInt(line[3]),
                        i
                ));
            }
            String[] robotActions = app.iteration();
        }

    }

    /**
     * Для каждого заказа находим ближайшего по реальному пути робота, отправляем его на заказ оптимальным путём
     *
     * @return список действий для каждого робота
     */
    private String[] iteration() {
        String[] robotsActions = new String[this.robots.size()];
        Arrays.fill(robotsActions, "");
        for (int step = 0; step < STEPS_PER_INTERACTION; step++) {

            // Если нет - ищем новый заказ
            robotsLoop:
            for (int i = 0; i < robots.size(); i++) {
                Robot robot = robots.get(i);
                // Если у робота есть заказ - идем к нему, или доставляем его
                if (robot.currentOrder != null) {
                    // Если робот собирается забрать заказ
                    if (robot.currentOrder.status == OrderStatus.PICKING) {
                        // Если робот уже на клетке с заказом
                        if (robot.curX == robot.currentOrder.sRow && robot.curY == robot.currentOrder.sCol) {
                            robotsActions[i] += 'T';
                            robot.currentOrder.status = OrderStatus.DELIVERING;
                            robot.path = findPath(robot.currentOrder.sRow, robot.currentOrder.sCol,
                                    robot.currentOrder.endRow, robot.currentOrder.endCol, cityMap);
                            continue robotsLoop;
                            // Если нет - идём к нему
                        } else {
                            Step nextStep = robot.path.remove();
                            robotsActions[i] += nextStep.action;
                            robot.curX = nextStep.x;
                            robot.curY = nextStep.y;
                            continue robotsLoop;
                        }
                        // Если робот уже доставляет заказ
                    } else if (robot.currentOrder.status == OrderStatus.DELIVERING) {
                        // Если робот уже в конечной точке
                        if (robot.curX == robot.currentOrder.endRow && robot.curY == robot.currentOrder.endCol) {
                            ordersPerIterations[robot.currentOrder.numberOfIteration]--;
                            robotsActions[i] += 'P';
                            orders.remove(robot.currentOrder);
                            robot.currentOrder = null;
                        } else {
                            Step nextStep = robot.path.remove();
                            robotsActions[i] += nextStep.action;
                            robot.curX = nextStep.x;
                            robot.curY = nextStep.y;
                            continue robotsLoop;
                        }
                    }
                }
                // А если у робота нет заказа
                else {
                    // Находим ближайшие из доступных заказов
                    Map<Order, Queue<Step>> orderDistanceMap = new HashMap<>();
                    List<Order> idleOrders = this.orders.stream().filter(o -> o.status == OrderStatus.IDLE).collect(Collectors.toList());
                    for (Order order : idleOrders) {
                        Queue<Step> path = findPath(robot.curX, robot.curY, order.sRow, order.sCol, cityMap);
                        if (path != null) orderDistanceMap.put(order, path);
                    }

                    List<Map.Entry<Order, Queue<Step>>> ordered = orderDistanceMap.entrySet().stream().sorted(Comparator.comparingInt(e -> e.getValue().size())).collect(Collectors.toList());
                    for (Map.Entry<Order, Queue<Step>> orderDistance: ordered) {
                        //TODO: проверка на то, что заказ самый старый в ячейке
                        //if (есть другие заказы и этот не самый старый в ячйке)
                    }

                    robotsActions[i] += 'S';

                }
            }
        }
        return robotsActions;
    }

    int findOldestIteration() {
        for (int i = 0; i < ordersPerIterations.length; i++) {
            if (ordersPerIterations[i] > 0) return i;
        }
        return -1;
    }

    private Queue<Step> findPath(int sRow, int sCol, int fRow, int fCol, boolean[][] cityMap) {
        Node start = new Node(sRow, sCol);
        boolean[][] visited = new boolean[cityMap.length][cityMap.length];
        Queue<Node> queue = new LinkedList<>();
        queue.add(start);
        while (!queue.isEmpty()) {
            Node node = queue.poll();
            if (node.x == fRow && node.y == fCol) {
                return node.trace;
            }
            visited[node.x][node.y] = true;
            // U
            if (cellIsValid(node.x - 1, node.y, cityMap, visited)) {
                Queue<Step> trace = node.trace;
                Step step = new Step('U', node.x - 1, node.y);
                trace.add(step);
                queue.add(new Node(node.x - 1, node.y, trace));
            }
            // L
            if (cellIsValid(node.x, node.y - 1, cityMap, visited)) {
                Queue<Step> trace = node.trace;
                Step step = new Step('L', node.x, node.y - 1);
                trace.add(step);
                queue.add(new Node(node.x, node.y - 1, trace));
            }
            // D
            if (cellIsValid(node.x + 1, node.y, cityMap, visited)) {
                Queue<Step> trace = node.trace;
                Step step = new Step('D', node.x + 1, node.y);
                trace.add(step);
                queue.add(new Node(node.x + 1, node.y, trace));
            }
            // R
            if (cellIsValid(node.x, node.y + 1, cityMap, visited)) {
                Queue<Step> trace = node.trace;
                Step step = new Step('R', node.x, node.y + 1);
                trace.add(step);
                queue.add(new Node(node.x, node.y + 1, trace));
            }
        }

        return null;

    }

    boolean cellIsValid(int x, int y, boolean[][] cityMap, boolean[][] visited) {
        return x >= 0 && y >= 0 && x < cityMap.length && y < cityMap.length && !cityMap[x][y] && !visited[x][y];
    }

    class Node {
        int x;
        int y;
        Queue<Step> trace;

        public Node(int x, int y) {
            this.x = x;
            this.y = y;
            this.trace = new LinkedList<>();
        }

        public Node(int x, int y, Queue<Step> trace) {
            this.x = x;
            this.y = y;
            this.trace = trace;
        }
    }


    private int[][] robotInitCoordinates(boolean[][] cityMap, int maxTips, long cost,
                                         int numberOfIterations, int totalNumberOfOrders) {
        int optimalNumberOfRovers = findOptimalNumberOfRovers(totalNumberOfOrders, numberOfIterations, maxTips, cost, cityMap);
        int[][] robotsInitCoordinates = new int[optimalNumberOfRovers][2];
        robotsLoop:
        for (int i = 0; i < optimalNumberOfRovers; i++) {
            coordsSearchLoop:
            while (true) {
                int x = getRand(0, cityMap.length - 1);
                int y = getRand(0, cityMap.length - 1);
                if (cityMap[x][y]) continue;
                for (int[] robotCoords : robotsInitCoordinates) {
                    // Ячейка уже занята
                    if (robotCoords[0] == x && robotCoords[1] == y) continue coordsSearchLoop;
                }
                robotsInitCoordinates[i] = new int[]{x, y};
                continue robotsLoop;
            }
        }

        return robotsInitCoordinates;
    }


    private int findOptimalNumberOfRovers(int totalNumberOfOrders, int numberOfIterations, int maxTips, long cost, boolean[][] cityMap) {
        // Посчитать сколько можно заработать за один круг в среднем
        // Умножить на количество кругов и вычесть количество роверов, умноженное на их цену

        int averageNumberOfOrders = totalNumberOfOrders / numberOfIterations;

        int freeCells = countFreeCells(cityMap);

        // Грубо предположим, что за один раунд ровер может доставить количество заказов, равное 60 доступным шагам ровера, поделенным на диагональ квадрата
        double ordersPerRoverFor1Interaction = STEPS_PER_INTERACTION / (Math.sqrt(2) * cityMap.length);


        double maxProfit = 0;
        int optimalRoversNumber = 1;

        // Иметь роверов больше чем заказов бессмысленно
        // Иметь роверов больше чем свободных клеток так же бессмысленно
        // Роверов не может быть больше 100
        // Грубо считаем сколько мы можем заработать за один раунд с искомым количеством роверов,
        // умножаем на количество раундов и вычитаем стоимость постройки этих роверов
        for (int roversNumber = 1; roversNumber <= averageNumberOfOrders
                && roversNumber <= freeCells
                && roversNumber <= MAX_ROBOTS; roversNumber++) {
            long roversCost = roversNumber * cost;
            double totalTips = (long) roversNumber * ordersPerRoverFor1Interaction * maxTips * numberOfIterations;
            double profit = totalTips - roversCost;
            if (profit > maxProfit) {
                maxProfit = profit;
                optimalRoversNumber = roversNumber;
            }
        }

        return optimalRoversNumber;
    }


    private int countFreeCells(boolean[][] cityMap) {
        int count = 0;
        for (int i = 0; i < cityMap.length; i++) {
            for (int j = 0; j < cityMap[i].length; j++) {
                if (!cityMap[i][j]) count++;
            }
        }

        return count;
    }

    private int getRand(int left, int right) {
        Random random = new Random();
        return random.ints(left, right)
                .findFirst()
                .getAsInt();
    }
}

class Robot {
    int curX;
    int curY;
    Order currentOrder;
    Queue<Step> path;

    Robot(int x, int y) {
        this.curX = x;
        this.curY = y;
        this.path = new LinkedList<>();
    }
}

class Order {
    int sRow;
    int sCol;
    int endRow;
    int endCol;
    OrderStatus status;
    int numberOfIteration;

    public Order(int sRow, int sCol, int endRow, int endCol, int numberOfInteraction) {
        this.sRow = sRow;
        this.sCol = sCol;
        this.endRow = endRow;
        this.endCol = endCol;
        this.numberOfIteration = numberOfInteraction;
        this.status = OrderStatus.IDLE;
    }
}

class Step {
    char action;
    int x;
    int y;

    public Step(char action, int x, int y) {
        this.action = action;
        this.x = x;
        this.y = y;
    }
}

enum OrderStatus {
    IDLE, PICKING, DELIVERING
}