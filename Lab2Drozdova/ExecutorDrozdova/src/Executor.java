import ru.spbstu.pipeline.IExecutable;
import ru.spbstu.pipeline.IExecutor;
import ru.spbstu.pipeline.RC;

import java.util.Arrays;
import java.util.logging.Logger;

public class Executor implements IExecutor {

    private final Logger LOG;
    private final RLE rle = new RLE();
    private boolean compression, recovery;
    private int lengthSequence;

    private final ExecutorGrammar grammar = new ExecutorGrammar(ExecutorSyntax.executorToken);
    private IExecutable producer;
    private IExecutable consumer;

    public Executor(Logger LOG) {
        this.LOG = LOG;
    }

    @Override
    public RC setConfig(String s) {
        if (s == null) {
            LOG.warning("Empty executor's config name string");
            return RC.CODE_INVALID_ARGUMENT;
        } else {
            RC error;
            ConfigParserExecutor configParserExecutor = new ConfigParserExecutor(grammar, LOG);
            String[] config = configParserExecutor.parser(s);
            error = configParserExecutor.getError();
            if(error != RC.CODE_SUCCESS) {
                return error;
            } else {
                lengthSequence = Integer.parseInt(config[Arrays.asList(ExecutorSyntax.executorToken).indexOf(ExecutorSyntax.lengthSequence)]);
                compression = Boolean.parseBoolean(config[Arrays.asList(ExecutorSyntax.executorToken).indexOf(ExecutorSyntax.compression)]);
                recovery = Boolean.parseBoolean(config[Arrays.asList(ExecutorSyntax.executorToken).indexOf(ExecutorSyntax.recovery)]);
                if (compression && recovery) {
                    LOG.warning("Invalid argument in executor's config: COMPRESSION and RECOVERY match");
                    return RC.CODE_INVALID_ARGUMENT;
                }

            }
        }
        return RC.CODE_SUCCESS;
    }

    @Override
    public RC setConsumer(IExecutable iExecutable) {
        if (iExecutable == null) {
            LOG.warning("Executor's consumer is null: is impossible to build pipeline");
            return RC.CODE_FAILED_PIPELINE_CONSTRUCTION;
        } else {
            consumer = iExecutable;
            return RC.CODE_SUCCESS;
        }
    }

    @Override
    public RC setProducer(IExecutable iExecutable) {
        if (iExecutable == null) {
            LOG.warning("Executor's producer is null: is impossible to build pipeline");
            return RC.CODE_FAILED_PIPELINE_CONSTRUCTION;
        } else {
            producer = iExecutable;
            return RC.CODE_SUCCESS;
        }
    }

    @Override
    public RC execute(byte[] bytes) {
        if (bytes == null) {
            LOG.warning("The execute function received null data");
            return RC.CODE_INVALID_ARGUMENT;
        } else {
            if (compression) {
                bytes = rle.RLECompression(bytes, lengthSequence);
            }
            if (recovery) {
                bytes = rle.RLERecovery(bytes);
            }
            RC error = consumer.execute(bytes);
            return error;
        }
    }
}

