/**
 * @author LiLin
 * @version 1.0
 * @date 2023/10/21 下午7:58
 */
import Connector.Connector;
import Connector.Result;
import Connector.SqlFileParser;
import MySql.MySqlLexer;
import MySql.MySqlParser;
import Oracle.BugReport;
import Oracle.Candidate;
import Oracle.OracleChecker;
import Oracle.PinoloVisitor;
import Strategy.*;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;


import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class MySqlTest {
    /**
     * 准备实验数据
     */
    public static List<ArgumentReader> initializeReaders(String rootDir) {
        List<ArgumentReader> readers = new ArrayList<>();

        File directory = new File(rootDir);
        for (File taskFolder : directory.listFiles()) {
            if (taskFolder.isDirectory() && taskFolder.getName().startsWith("task-")) {
                String taskFolderPath = taskFolder.getAbsolutePath();
                String ddlPath = taskFolderPath + "/output.data.sql";
                String json1Path = taskFolderPath + "/result.json";

                File bugsFolder = new File(taskFolderPath + "/bugs");
                if (bugsFolder.exists() && bugsFolder.isDirectory()) {
                    for (File file : bugsFolder.listFiles()) {
                        if (file.getName().endsWith(".json")) {
                            String baseName = file.getName().substring(0, file.getName().length() - 5);
                            String logPath = bugsFolder.getAbsolutePath() + "/" + baseName + ".log";
                            int lastIndex = rootDir.lastIndexOf('/');
                            String database = "mojito";
                            if (lastIndex != -1) {
                                database= rootDir.substring(lastIndex + 1);
                            }
                            String outputPath = "/media/linlideepin/Data/learning/db/SQLess/src/output" + "/" + database + "/" + baseName + ".json";

                            if (new File(logPath).exists()) {
                                readers.add(new ArgumentReader(json1Path, file.getAbsolutePath(), logPath, ddlPath, outputPath));
                            }
                        }
                    }
                }
            }
        }

        return readers;
    }
    public static void main(String[] args) {
//        String[] argss = new String[]{"/media/linlideepin/Data/learning/db/实验/impomysqlres/output4/mysql/task-8/result.json",
//                "/media/linlideepin/Data/learning/db/实验/impomysqlres/output4/mysql/task-8/bugs/bug-1-1596-FixMDistinctL.json",
//                "/media/linlideepin/Data/learning/db/实验/impomysqlres/output4/mysql/task-8/bugs/bug-1-1596-FixMDistinctL.log",
//                "/media/linlideepin/Data/learning/db/实验/impomysqlres/output4/mysql/task-8/output.data.sql",
//                "/media/linlideepin/Data/learning/db/SQLess/src/output/slimresult.json"};
//        ArgumentReader arg = news ArgumentReader("/media/linlideepin/Data/learning/db/实验/impomysqlres/output1/mariadb/task-6/result.json",
//                "/media/linlideepin/Data/learning/db/实验/impomysqlres/output1/mariadb/task-6/bugs/bug-0-102-FixMHaving1U.json",
//                "/media/linlideepin/Data/learning/db/实验/impomysqlres/output1/mariadb/task-6/bugs/bug-0-102-FixMHaving1U.log",
//                "/media/linlideepin/Data/learning/db/实验/impomysqlres/output1/mariadb/task-6/output.data.sql",
//                "./");

//        ArgumentReader arg = new ArgumentReader("/media/linlideepin/Data/learning/db/实验/impomysqlres/output1/mysql/task-0/result.json",
//                "/media/linlideepin/Data/learning/db/实验/impomysqlres/output1/mysql/task-0/bugs/bug-22-12193-FixMHaving1U.json",
//                "/media/linlideepin/Data/learning/db/实验/impomysqlres/output1/mysql/task-0/bugs/bug-22-12193-FixMHaving1U.log",
//                "/media/linlideepin/Data/learning/db/实验/impomysqlres/output1/mysql/task-0/output.data.sql",
//                "./");
//
//        task(arg);

        List<ArgumentReader> mysqlargumentReaders = initializeReaders("/media/linlideepin/Data/learning/db/实验/impomysqlres/output1/mysql");
//        List<ArgumentReader> tidbargumentReaders = initializeReaders("/media/linlideepin/Data/learning/db/实验/impomysqlres/output1/tidb");
//        List<ArgumentReader> oceanbaseargumentReaders = initializeReaders("/media/linlideepin/Data/learning/db/实验/impomysqlres/output1/oceanbase");
//        List<ArgumentReader> mariadbargumentReaders = initializeReaders("/media/linlideepin/Data/learning/db/实验/impomysqlres/output1/mariadb");
        // 创建线程池
        ExecutorService executor = Executors.newFixedThreadPool(2); //任务并行

        // 提交MySQL测试任务到线程池
        executor.submit(() -> TestDBMS(mysqlargumentReaders, 13306));

        // 提交TiDB测试任务到线程池
//        executor.submit(() -> TestDBMS(tidbargumentReaders, 4000));

        // 提交Oceanbase测试任务到线程池
//        executor.submit(() -> TestDBMS(oceanbaseargumentReaders, 2881));

        // 提交Mariadb测试任务到线程池
//        executor.submit(() -> TestDBMS(mariadbargumentReaders, 9901));  //mariadb的客户端不能使用Mysql连接

        // 关闭线程池
        executor.shutdown();

        try {
            // 等待任务完成，或者超时，或者线程被中断
            if (!executor.awaitTermination(10000, TimeUnit.HOURS)) {  //原本设置1000
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public static void TestDBMS(List<ArgumentReader> argumentReaders,int port){
        for(ArgumentReader arg:argumentReaders){
//            long startTime = System.currentTimeMillis();
            task(arg,port);
//            long endTime = System.currentTimeMillis();
//            long duration = endTime - startTime;
//            System.out.println("执行时间: " + duration + " 毫秒");
        }
    }

    /**
     * 0 Init DBMS
     * 1 Verify Bugs
     * 2 Sql SLim
     * 3 Mutate
     * 4 Check Oracles
     * 5 output BugReport
     */
    public static void task(ArgumentReader argumentReader,Integer port){
        try{
            /**
             * 0 Init DBMS and load bug
             */
            // 0.1 从JSON文件读取数据并创建BugReport实例
            BugReport bugReport;
//            BugReport bugReport = BugReport.DataLoader("/media/linlideepin/Data/learning/db/实验/impomysqlres/output1/mysql/task-7/result.json",
//                    "/media/linlideepin/Data/learning/db/实验/impomysqlres/output1/mysql/task-7/bugs/bug-0-90-FixMDistinctL.json",
//                    "/media/linlideepin/Data/learning/db/实验/impomysqlres/output1/mysql/task-7/bugs/bug-0-90-FixMDistinctL.log",
//                    "/media/linlideepin/Data/learning/db/实验/impomysqlres/output1/mysql/task-7/output.data.sql");  //精简效果不错
//
//            bugReport = BugReport.DataLoader("/media/linlideepin/Data/learning/db/实验/impomysqlres/output4/mysql/task-8/result.json",
//                    "/media/linlideepin/Data/learning/db/实验/impomysqlres/output4/mysql/task-8/bugs/bug-1-1596-FixMDistinctL.json",
//                    "/media/linlideepin/Data/learning/db/实验/impomysqlres/output4/mysql/task-8/bugs/bug-1-1596-FixMDistinctL.log",
//                    "/media/linlideepin/Data/learning/db/实验/impomysqlres/output4/mysql/task-8/output.data.sql");
            String outputPath = "./";
            bugReport = BugReport.DataLoader(argumentReader.getJson1path(), argumentReader.getJson2path(), argumentReader.getLogpath(), argumentReader.getDdlpath());
            bugReport.setPort(port);
            outputPath = argumentReader.getOutputpath();

//            System.out.println(bugReport);
            System.out.println(bugReport.getOriginalSql());
            // 0.2 建立数据库连接
            Connector connector = new Connector(
                    bugReport.getDbms(),
                    bugReport.getHost(),
                    bugReport.getPort(),
                    bugReport.getUsername(),
                    bugReport.getPassword(),
                    bugReport.getDbname()
            );
            // 0.3 读取并执行DDL语句初始化数据库
            String ddlPath = bugReport.getDdlPath();
            SqlFileParser p = new SqlFileParser(ddlPath);
            p.parse();
            connector.execSQL("use TEST3");
            List<String> initsqls = p.getResult();
            for(int i=0;i<initsqls.size();i++){
                String initsql = initsqls.get(i);
//                System.out.println(initsql);
                connector.execSQL(initsql);
            }

            /**
             * 1 Verify Bugs
             */

            // 1.1 执行originalSql并获取结果
            String originalSql = bugReport.getOriginalSql();
            Result originalResult = connector.execSQL(originalSql);

            // 1.2 执行mutatedSql并获取结果
            String mutatedSql = bugReport.getMutatedSql();
            Result mutatedResult = connector.execSQL(mutatedSql);

            // 1.3 比较两个SQL查询的结果
            System.out.println("Comparing results");
            System.out.println(bugReport.getMutationName());
            originalResult.getResult();
            mutatedResult.getResult();
            if(bugReport.getMutationName().equals("pinonlo")){
                if(OracleChecker.Pinolo_check(originalResult,mutatedResult,bugReport.isUpper())){
                    System.out.println("Not a bug");
                }else{
                    System.out.println("Is Bug");
                }
            }

            //1.4  比较一下我们的Oracle是否能找到Bug
            Mutate(originalSql);
            boolean isreproduce = false;
            for(Candidate candidate:Candidate.candidates){
                //对于每个候选变异SQL语句执行并检查结果
                Result originalRes = connector.execSQL(originalSql);
                Result mutateRes = connector.execSQL(candidate.mutateSql);
                int finalIsUpper = (candidate.isUpper ^ candidate.flag) ^ 1;
                //符合Oracle，则替换，并进行下一轮的精简
                if(bugReport.getMutationName().equals("pinonlo")) {
                    if (OracleChecker.Pinolo_check(originalRes, mutateRes, finalIsUpper == 1)) {
                    } else {
                        isreproduce = true;
                        break;
                    }
                }
            }
            if(isreproduce){
                System.out.println("The bug is reproduced! and Is a bug!");
            }else{
                System.out.println("Not reproduce bug!");
                return ;
            }


            /**
             * 2 在这里实现Sql精简逻辑,读入精简策略
             */
            ConfigReader reader = new ConfigReader();
            List<String> simplify = reader.readSimplificationStrategies("/home/linlideepin/Desktop/db 快捷方式/SQLess/src/main/java/strategy.xml");
            for (String sim: simplify) {
                System.out.println(sim);
            }

            List<String> originsqls = new ArrayList<>();
            originsqls.add(originalSql);
            originsqls.add(mutatedSql);

            String SlimOriginalSql = originalSql;
            String SlimmutatedSql = mutatedSql;
            // 2.1 遍历精简策略
            for (int i=0;i<simplify.size();i++){
                String simplifyStrategy = simplify.get(i);
                System.out.println("Now we are simplify with strategy:" + simplifyStrategy);
                if(simplifyStrategy.equals(SimplificationStrategy.REMOVECOLUMN)
                 || simplifyStrategy.equals(SimplificationStrategy.REMOVEBITOPLEFT)
                 || simplifyStrategy.equals(SimplificationStrategy.REMOVELOGICALOPLEFT)
                 || simplifyStrategy.equals(SimplificationStrategy.REMOVELOGICALOPRIGHT)
                 || simplifyStrategy.equals(SimplificationStrategy.REMOVEBITOPRIGHT)
                 || simplifyStrategy.equals(SimplificationStrategy.REMOVEGROUPBY)
                 || simplifyStrategy.equals(SimplificationStrategy.REMOVEHAVING)
                 || simplifyStrategy.equals(SimplificationStrategy.REMOVELIMIT)
                 || simplifyStrategy.equals(SimplificationStrategy.REMOVEORDERBY)
                 || simplifyStrategy.equals(SimplificationStrategy.REMOVEWHERE)
                ){   //for example: try to remove selectElement i(1,2,3)
                    int jumpindex = 0;  //精简第几处clause
                    outerLoop:
                    while(true){
                        //2.2 返回精简的SQL语句
                        String Slimtemp = simplifySql(SlimOriginalSql,simplifyStrategy,jumpindex);
                        System.out.println("SlimResult:" + Slimtemp);
                        if(Slimtemp.equals(SlimOriginalSql)){
                            break;    // 没有精简，已经跳过了所有的clause了，可以退出循环
                        }
                        /**
                         * 3 变异，变异结果保存到Cadidate类的candidates静态变量中
                         */
                        Mutate(Slimtemp);
                        /**
                         * 4 Check Oracle
                         */
                        for(Candidate candidate:Candidate.candidates){
                            // 4.1 对于每个候选变异SQL语句执行并检查结果
                            Result SlimoriginalRes = connector.execSQL(Slimtemp);
                            Result SlimmutateRes = connector.execSQL(candidate.mutateSql);
//                            System.out.println("candidate:" + candidate.mutateSql);
                            int finalIsUpper = (candidate.isUpper ^ candidate.flag) ^ 1;
                            // 4.4 符合Oracle，则替换，并进行下一轮的精简
                            if(bugReport.getMutationName().equals("pinonlo")) {
                                if (OracleChecker.Pinolo_check(SlimoriginalRes, SlimmutateRes, finalIsUpper == 1)) {
//                                    System.out.println("Not a bug");
                                } else {
                                    SlimOriginalSql =Slimtemp;
                                    SlimmutatedSql = candidate.mutateSql;
                                    System.out.println("Is Bug");
                                    break outerLoop;                        // 如果候选的变异算子检测到一个Bug，就跳过
                                }
                            }
                        }
                        jumpindex++ ;  //所有变异都没有检测出来，则此次精简无效
                    }
                }else{
                    //2.2 返回精简的SQL语句
                    String Slimtemp = simplifySql(SlimOriginalSql,simplifyStrategy,0);
                    System.out.println("SlimResult:" + Slimtemp);
                    /**
                     * 3 变异，变异结果保存到Cadidate类的candidates静态变量中
                     */
                    Mutate(Slimtemp);
                    /**
                     * 4 Check Oracle
                     */
                    for(Candidate candidate:Candidate.candidates){
                        // 4.1 对于每个候选变异SQL语句执行并检查结果
                        Result SlimoriginalRes = connector.execSQL(Slimtemp);
                        Result SlimmutateRes = connector.execSQL(candidate.mutateSql);
                        int finalIsUpper = (candidate.isUpper ^ candidate.flag) ^ 1;
                        // 4.4 符合Oracle，则替换，并进行下一轮的精简
                        if(bugReport.getMutationName().equals("pinonlo")) {
                            if (OracleChecker.Pinolo_check(SlimoriginalRes, SlimmutateRes, finalIsUpper == 1)) {
//                                System.out.println("Not a bug");
                            } else {
                                SlimOriginalSql = Slimtemp;
                                SlimmutatedSql = candidate.mutateSql;
                                System.out.println("Is Bug");
                                break;
                            }
                        }
                    }
                }
            }
            System.out.println("------------------");
            System.out.println(SlimOriginalSql);
            System.out.println("------------------");
            System.out.println(SlimmutatedSql);
            bugReport.setSlimoriginalSql(SlimOriginalSql);
            bugReport.setSlimmutatedSql(SlimmutatedSql);

            /**
             * 5 输出BugReport
             */
            bugReport.writeToFile(outputPath);
            // 关闭连接
            connector.close();
        }catch (IOException e) {
            System.err.println("Error reading from JSON file or DDL file: " + e.getMessage());
            e.printStackTrace();
        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     *
     * @param Sql1
     * @param strategy
     * @param jumpindex   精简第i处clause
     * @return
     * @throws SQLException
     */
    public static String simplifySql(String Sql1,String strategy,int jumpindex) throws SQLException {
        String originalSql = Sql1;
        String SlimOriginalSql = originalSql;
        CommonTokenStream tokens1 = SQLtoTokens(originalSql);
        ParseTree tree1 = TokenToParserTree(tokens1);

        // 1. 解析SQL语句
        // 2. 遍历和修改AST
         if(strategy.equals(SimplificationStrategy.REMOVEORDERBY)) {  //remove order by
             RemoveOrderByVisitor visitor1 = new RemoveOrderByVisitor(tokens1,jumpindex+1);
             visitor1.visit(tree1);
             SlimOriginalSql = visitor1.getText();
         }else if (strategy.equals(SimplificationStrategy.REMOVEWITH)) {  //remove with
             if(originalSql.trim().toUpperCase().startsWith("WITH")) {
                 RemoveWithStatementVisitor visitor1 = new RemoveWithStatementVisitor(tokens1);
                 visitor1.visit(tree1);
                 SlimOriginalSql = visitor1.getText();
//                 RuleNode newTree1 = visitor1.getNewRoot();
//                 SlimOriginalSql = newTree1.getText();
//                 SlimOriginalSql = SQLConstructVisitor.getSQLfromAST((RuleNode) newTree1);
             }
         }else if(strategy.equals(SimplificationStrategy.REMOVEUSEINDEX)){   //remove use index
             RemoveUseIndexVisitor visitor1 = new RemoveUseIndexVisitor(tokens1);
             visitor1.visit(tree1);
             SlimOriginalSql = visitor1.getModifiedText();
         }else if(strategy.equals(SimplificationStrategy.REMOVEUNIONLEFT)){  //remove union left
             RemoveUnionVisitor visitor1 = new RemoveUnionVisitor(tokens1,0);
             visitor1.visit(tree1);
             SlimOriginalSql = visitor1.getText();
         }else if (strategy.equals(SimplificationStrategy.REMOVEUNIONRIGHT)){  // remove union right
             RemoveUnionVisitor visitor1 = new RemoveUnionVisitor(tokens1,1);
             visitor1.visit(tree1);
             SlimOriginalSql = visitor1.getText();
         } else if(strategy.equals(SimplificationStrategy.REMOVEGROUPBY)){  // remove group by
             RemoveGroupByVisitor visitor1 = new RemoveGroupByVisitor(tokens1,jumpindex+1);
             visitor1.visit(tree1);
             SlimOriginalSql = visitor1.getText();
         }else if(strategy.equals(SimplificationStrategy.REMOVEHAVING)){  // remove having
             RemoveHavingVisitor visitor1 = new RemoveHavingVisitor(tokens1,jumpindex+1);
             visitor1.visit(tree1);
             SlimOriginalSql = visitor1.getText();
         }else if(strategy.equals(SimplificationStrategy.REMOVELIMIT)){  //remove limit
             RemoveLimitVisitor visitor1 = new RemoveLimitVisitor(tokens1,jumpindex+1);
             visitor1.visit(tree1);
             SlimOriginalSql = visitor1.getText();
         }else if(strategy.equals(SimplificationStrategy.REMOVEWHERE)){  //remove where
             RemoveWhereVisitor visitor1 = new RemoveWhereVisitor(tokens1,jumpindex+1);
             visitor1.visit(tree1);
             SlimOriginalSql = visitor1.getText();
         }else if(strategy.equals(SimplificationStrategy.REMOVECOLUMN)){  //remove column (不够精简）
             //这里需要先判断一些是否包含Union，如果包含Union，需要拆分成两个select子句分别分析；
             ExtractUnionPartsVisitor euv = new ExtractUnionPartsVisitor(tokens1);
             euv.visit(tree1);
             if(euv.isContainsUnion()){  //如果包含Union则需要拆分成两个子句删除列
                 String unionleft = euv.getLeftPart();
                 String unionmiddle = euv.getMiddlePart();
                 String unionright = euv.getRightPart();
                 CommonTokenStream ultokens = SQLtoTokens(unionleft);
                 CommonTokenStream urtokens = SQLtoTokens(unionright);
                 ParseTree ultree = TokenToParserTree(ultokens);
                 ParseTree urtree = TokenToParserTree(urtokens);

                 AliasVisitor avisitorl = new AliasVisitor();    //Union left删除列  同步删除
                 avisitorl.visit(ultree);
                 Graph ulgraph = avisitorl.getGraph();
                 ConstructDG ulconsDG = new ConstructDG(ulgraph);
                 ulconsDG.visit(ultree);
                 ulgraph = ulconsDG.getGraph();
                 RemoveColumnVisitor ulvisitor = new RemoveColumnVisitor(ultokens,jumpindex,ulgraph);
                 ulvisitor.visit(ultree);
                 unionleft = ulvisitor.getText();

                 AliasVisitor avisitorr = new AliasVisitor();    //Union right删除列
                 avisitorr.visit(urtree);
                 Graph urgraph = avisitorr.getGraph();
                 ConstructDG urconsDG = new ConstructDG(urgraph);
                 urconsDG.visit(urtree);
                 urgraph = urconsDG.getGraph();
                 RemoveColumnVisitor urvisitor = new RemoveColumnVisitor(urtokens,jumpindex,urgraph);
                 urvisitor.visit(urtree);
                 unionright = urvisitor.getText();

                 SlimOriginalSql = unionleft.replace(';',' ') + " " + unionmiddle.replace(';',' ') + " " + unionright;

             }else{
                 //精简前先进行别名分析、依赖分析构造DG
                 AliasVisitor avisitor = new AliasVisitor();
                 avisitor.visit(tree1);
                 Graph graph1 = avisitor.getGraph();
                 ConstructDG consDG = new ConstructDG(graph1);
                 consDG.visit(tree1);
                 graph1 = consDG.getGraph();
                 RemoveColumnVisitor visitor1 = new RemoveColumnVisitor(tokens1,jumpindex,graph1);
                 visitor1.visit(tree1);
                 SlimOriginalSql = visitor1.getText();
             }
         }else if(strategy.equals(SimplificationStrategy.REMOVEUNUSEDEF)){  //remove unused def
             String slimOriginalSql = Sql1;
             ExtractUnionPartsVisitor euv = new ExtractUnionPartsVisitor(tokens1);
             euv.visit(tree1);
             if(euv.isContainsUnion()){
                 String unionleft = euv.getLeftPart();
                 String unionmiddle = euv.getMiddlePart();
                 String unionright = euv.getRightPart();
                 unionleft = removedUnusedDef(unionleft);
                 unionright = removedUnusedDef(unionright);
                 slimOriginalSql = unionleft.replace(';',' ') + " " + unionmiddle.replace(';',' ') + " " + unionright;
             }else{
                 slimOriginalSql = removedUnusedDef(slimOriginalSql);
             }
             SlimOriginalSql = slimOriginalSql;
         }else if(strategy.equals(SimplificationStrategy.REMOVEBITOPLEFT)){  //remove bitop left
             RemoveBitOpVisitor visitor1 = new RemoveBitOpVisitor(tokens1, RemoveBitOpVisitor.OperandSide.LEFT,jumpindex+1);
             visitor1.visit(tree1);
             SlimOriginalSql = visitor1.getText();
         }else if(strategy.equals(SimplificationStrategy.REMOVEBITOPRIGHT)){  //remove bitop right
             RemoveBitOpVisitor visitor1 = new RemoveBitOpVisitor(tokens1, RemoveBitOpVisitor.OperandSide.RIGHT,jumpindex+1);
             visitor1.visit(tree1);
             SlimOriginalSql = visitor1.getText();
         }else if(strategy.equals(SimplificationStrategy.REMOVELOGICALOPLEFT)){  //remove logical op left
             RemoveLogicalOpVisitor visitor1 = new RemoveLogicalOpVisitor(tokens1, RemoveLogicalOpVisitor.OperandSide.LEFT,jumpindex+1);
             visitor1.visit(tree1);
             SlimOriginalSql = visitor1.getText();
         }else if(strategy.equals(SimplificationStrategy.REMOVELOGICALOPRIGHT)){  //remove logical op right
             RemoveLogicalOpVisitor visitor1 = new RemoveLogicalOpVisitor(tokens1, RemoveLogicalOpVisitor.OperandSide.RIGHT,jumpindex+1);
             visitor1.visit(tree1);
             SlimOriginalSql = visitor1.getText();
         }else if(strategy.equals(SimplificationStrategy.REMOVESQLHINT)){  //remove sql hint
             SlimOriginalSql = RemoveSqlHint.removeOptimizationHints(tokens1);
         }
        return SlimOriginalSql;
    }

    public static CommonTokenStream SQLtoTokens(String sql){
        MySqlLexer lexer = new MySqlLexer(CharStreams.fromString(sql));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        return tokens;
    }

    public static ParseTree TokenToParserTree(CommonTokenStream tokens){
        MySqlParser parser = new MySqlParser(tokens);
        ParseTree tree = parser.root();
        return tree;
    }

    public static void Mutate(String OriginalSql){
        CommonTokenStream tokens =  SQLtoTokens(OriginalSql);
        ParseTree tree = TokenToParserTree(tokens);
        PinoloVisitor visitor = new PinoloVisitor(tokens);
        Candidate.clear();
        visitor.visit(tree);
    }

    public static String removedUnusedDef(String Sql1){
        String slimOriginalSql = Sql1;
        do {
            MySqlLexer lexer = new MySqlLexer(CharStreams.fromString(slimOriginalSql));
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            MySqlParser parser = new MySqlParser(tokens);
            ParseTree tree = parser.root();
            AliasVisitor visitor = new AliasVisitor();
            visitor.visit(tree);
            Graph graph = visitor.getGraph();
            ConstructDG dg = new ConstructDG(graph);
            dg.visit(tree);
            graph = dg.getGraph();

            RemoveUnusedDefVisitor visitor1 = new RemoveUnusedDefVisitor(tokens, graph);
            visitor1.visit(tree);
            String newSql = visitor1.getText();
            if(newSql.equals(slimOriginalSql)) break;
            slimOriginalSql = newSql;
        }while (true);
        return slimOriginalSql;
    }
}
