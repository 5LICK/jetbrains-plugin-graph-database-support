package com.neueda.jetbrains.plugin.graphdb.jetbrains.ui.console.log;

import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.util.messages.MessageBus;
import com.neueda.jetbrains.plugin.graphdb.database.api.query.GraphQueryResult;
import com.neueda.jetbrains.plugin.graphdb.jetbrains.actions.execute.ExecuteQueryPayload;
import com.neueda.jetbrains.plugin.graphdb.jetbrains.component.datasource.DataSource;
import com.neueda.jetbrains.plugin.graphdb.jetbrains.ui.console.GraphConsoleView;
import com.neueda.jetbrains.plugin.graphdb.jetbrains.ui.console.event.QueryExecutionProcessEvent;
import com.neueda.jetbrains.plugin.graphdb.jetbrains.ui.datasource.metadata.MetadataRetrieveEvent;

import java.awt.BorderLayout;

public class LogPanel implements Disposable {

    private ConsoleView log;

    public void initialize(GraphConsoleView graphConsoleView, Project project) {
        MessageBus messageBus = project.getMessageBus();

        log = TextConsoleBuilderFactory.getInstance()
                .createBuilder(project)
                .getConsole();
        Disposer.register(graphConsoleView, log);
        graphConsoleView.getLogTab().add(log.getComponent(), BorderLayout.CENTER);

        messageBus.connect().subscribe(QueryExecutionProcessEvent.QUERY_EXECUTION_PROCESS_TOPIC, new QueryExecutionProcessEvent() {
            @Override
            public void executionStarted(ExecuteQueryPayload payload) {
                info("Executing query: ");
                userInput(payload.getContent());
                newLine();
            }

            @Override
            public void resultReceived(GraphQueryResult result) {
                info(String.format("Query executed in %sms. %s", result.getExecutionTimeMs(), result.getResultSummary()));
                newLine();
            }

            @Override
            public void postResultReceived() {
            }

            @Override
            public void handleError(Exception exception) {
                error("Error occurred: ");
                printException(exception);
            }

            @Override
            public void executionCompleted() {
                newLine();
            }
        });

        messageBus.connect().subscribe(MetadataRetrieveEvent.METADATA_RETRIEVE_EVENT, new MetadataRetrieveEvent() {
            @Override
            public void startMetadataRefresh(DataSource nodeDataSource) {
                info(String.format("DataSource[%s] - refreshing metadata...", nodeDataSource.getName()));
                newLine();
            }

            @Override
            public void metadataRefreshSucceed(DataSource nodeDataSource) {
                info(String.format("DataSource[%s] - metadata refreshed successfully!", nodeDataSource.getName()));
                newLine();
                newLine();
            }

            @Override
            public void metadataRefreshFailed(DataSource nodeDataSource, Exception exception) {
                error(String.format("DataSource[%s] - metadata refresh failed. Reason: ", nodeDataSource.getName()));
                printException(exception);
                newLine();
            }
        });
    }

    public void userInput(String message) {
        log.print(message, ConsoleViewContentType.USER_INPUT);
    }

    public void info(String message) {
        log.print(message, ConsoleViewContentType.NORMAL_OUTPUT);
    }

    public void error(String message) {
        log.print(message, ConsoleViewContentType.ERROR_OUTPUT);
    }

    public void printException(Exception exception) {
        error(exception.getMessage());
        newLine();

        Throwable cause = exception.getCause();
        while (cause != null) {
            error(cause.getMessage());
            newLine();
            cause = cause.getCause();
        }
    }

    public void newLine() {
        log.print("\n", ConsoleViewContentType.NORMAL_OUTPUT);
    }

    @Override
    public void dispose() {
        log.dispose();
    }
}
