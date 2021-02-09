import ru.spbstu.pipeline.IExecutable;
import ru.spbstu.pipeline.IWriter;
import ru.spbstu.pipeline.RC;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Logger;

import static java.lang.Integer.min;
import static java.lang.Integer.parseInt;

public class Writer implements IWriter {

    private FileOutputStream fileOutput;
    private int buffetSize;
    private final Logger LOG;
    private final WriterGrammar grammar = new WriterGrammar(WriterSyntax.writerToken);
    private IExecutable producer;

    public Writer(Logger LOG) {
        this.LOG = LOG;
    }

    @Override
    public RC setOutputStream(FileOutputStream fileOutputStream) {
        if(fileOutputStream == null) {
            LOG.warning("Invalid output stream");
            return RC.CODE_INVALID_INPUT_STREAM;
        } else {
            fileOutput = fileOutputStream;
            return RC.CODE_SUCCESS;
        }
    }

    @Override
    public RC setConfig(String s) {
        if (s == null) {
            LOG.warning("Empty writer's config name string");
            return RC.CODE_INVALID_ARGUMENT;
        } else {
            ConfigParserWriter configParserWriter = new ConfigParserWriter(grammar, LOG);
            RC error;
            String[] config = configParserWriter.parser(s);
            error = configParserWriter.getError();
            if (error == RC.CODE_SUCCESS) {
                buffetSize = parseInt(config[Arrays.asList(WriterSyntax.writerToken).indexOf(WriterSyntax.bufferSize)]);
                if (buffetSize <= 0) {
                    LOG.warning("Invalid argument in writer's config: BUFFER_SIZE less than zero");
                    return RC.CODE_CONFIG_SEMANTIC_ERROR;
                }
            }
            return error;
        }
    }

    @Override
    public RC setConsumer(IExecutable consumer) {
        return RC.CODE_SUCCESS;
    }

    @Override
    public RC setProducer(IExecutable producer) {
        if(producer == null) {
            LOG.warning("Writer's consumer is null: is impossible to build pipeline");
            return RC.CODE_FAILED_PIPELINE_CONSTRUCTION;
        } else {
            this.producer = producer;
            return RC.CODE_SUCCESS;
        }
    }

    @Override
    public RC execute(byte[] data) {
        if (data == null) {
            LOG.warning("Worker received a null array of data");
            return RC.CODE_INVALID_ARGUMENT;
        } else {
            byte[] buffer;
            for (int i = 0; i < data.length; i = i + buffetSize) {
                int currentBufferSize = min(buffetSize, data.length - i);
                buffer = Arrays.copyOfRange(data, i, i + currentBufferSize);
                try {
                    fileOutput.write(buffer);
                } catch (IOException exception) {
                    LOG.warning("Error writing output file");
                    return RC.CODE_FAILED_TO_WRITE;
                }
            }
            return RC.CODE_SUCCESS;
        }
    }
}
