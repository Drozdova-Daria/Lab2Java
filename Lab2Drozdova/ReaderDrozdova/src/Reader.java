import ru.spbstu.pipeline.IExecutable;
import ru.spbstu.pipeline.IReader;
import ru.spbstu.pipeline.RC;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Logger;
import static java.lang.Integer.parseInt;

public class Reader implements IReader {

    private FileInputStream fileInput;
    private int buffetSize;
    private final Logger LOG;
    private final ReaderGrammar grammar = new ReaderGrammar(ReaderSyntax.readerToken);
    private IExecutable nextWorker;

    public Reader(Logger LOG) {
        this.LOG = LOG;
    }

    @Override
    public RC setInputStream(FileInputStream fileInputStream) {
        if(fileInputStream == null) {
            LOG.warning("Invalid input stream");
            return RC.CODE_INVALID_INPUT_STREAM;
        } else {
            fileInput = fileInputStream;
            return RC.CODE_SUCCESS;
        }
    }

    @Override
    public RC setConfig(String s) {
        if (s == null) {
            LOG.warning("Empty reader's config name string");
            return RC.CODE_INVALID_ARGUMENT;
        } else {
            ConfigParserReader configParserReader = new ConfigParserReader(grammar, LOG);
            RC error;
            String[] config = configParserReader.parser(s);
            error = configParserReader.getError();
            if (error == RC.CODE_SUCCESS) {
                buffetSize = parseInt(config[Arrays.asList(ReaderSyntax.readerToken).indexOf(ReaderSyntax.bufferSize)]);
                if (buffetSize <= 0) {
                    LOG.warning("Invalid argument in reader's config: BUFFER_SIZE less than zero");
                    return RC.CODE_INVALID_ARGUMENT;
                }
            }
            return error;
        }
    }

    @Override
    public RC setConsumer(IExecutable consumer) {
        if(consumer == null) {
            LOG.warning("Reader's consumer is null: is impossible to build pipeline");
            return RC.CODE_FAILED_PIPELINE_CONSTRUCTION;
        } else {
            nextWorker = consumer;
            return RC.CODE_SUCCESS;
        }
    }

    @Override
    public RC setProducer(IExecutable producer) {
        return RC.CODE_SUCCESS;
    }

    @Override
    public RC execute(byte[] data) {
        if (data == null) {
            try {
                while (fileInput.available() > 0) {
                    int currentBufferSize = Math.min(buffetSize, fileInput.available());
                    byte[] bytes = new byte[currentBufferSize];
                    if (fileInput.read(bytes) == -1) {
                        LOG.warning("No data in input file");
                        return RC.CODE_FAILED_TO_READ;
                    }
                    RC error = nextWorker.execute(bytes);
                    if (error != RC.CODE_SUCCESS) {
                        return error;
                    }
                }
            } catch (IOException exception) {
                LOG.warning("Error reading the input file");
                return RC.CODE_FAILED_TO_READ;
            }
        } else {
            LOG.warning("Worker received a null array of data");
            return RC.CODE_INVALID_ARGUMENT;
        }
        return RC.CODE_SUCCESS;
    }

}