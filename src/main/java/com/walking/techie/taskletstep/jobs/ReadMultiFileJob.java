package com.walking.techie.taskletstep.jobs;

import com.walking.techie.taskletstep.model.Domain;
import com.walking.techie.taskletstep.tasklet.FileDeletingTasklet;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.MultiResourceItemReader;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.BeanWrapperFieldExtractor;
import org.springframework.batch.item.file.transform.DelimitedLineAggregator;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

@Configuration
@EnableBatchProcessing
public class ReadMultiFileJob {

  @Autowired
  private JobBuilderFactory jobBuilderFactory;
  @Autowired
  private StepBuilderFactory stepBuilderFactory;

  @Value("csv/domain*.csv")
  private Resource[] resources;

  @Value("output/domain.all.csv")
  private Resource resource;

  @Value("csv/")
  private Resource directory;

  @Bean
  public Job readFiles() {
    return jobBuilderFactory.get("readFiles").incrementer(new RunIdIncrementer()).
        flow(step1()).next(step2()).end().build();
  }

  @Bean
  public Step step1() {
    return stepBuilderFactory.get("step1").<Domain, Domain>chunk(10)
        .reader(multiResourceItemReader()).writer(writer()).build();
  }

  @Bean
  public Step step2() {
    return stepBuilderFactory.get("step2").tasklet(fileDeletingTasklet()).build();
  }

  @Bean
  public FileDeletingTasklet fileDeletingTasklet() {
    FileDeletingTasklet tasklet = new FileDeletingTasklet();
    tasklet.setDirectory(directory);
    return tasklet;
  }

  @Bean
  public MultiResourceItemReader<Domain> multiResourceItemReader() {
    MultiResourceItemReader<Domain> resourceItemReader = new MultiResourceItemReader<Domain>();
    resourceItemReader.setResources(resources);
    resourceItemReader.setDelegate(reader());
    return resourceItemReader;
  }

  @Bean
  public FlatFileItemReader<Domain> reader() {
    FlatFileItemReader<Domain> reader = new FlatFileItemReader<Domain>();
    reader.setLineMapper(new DefaultLineMapper() {{
      setLineTokenizer(new DelimitedLineTokenizer() {{
        setNames(new String[]{"id", "domain"});
      }});
      setFieldSetMapper(new BeanWrapperFieldSetMapper<Domain>() {{
        setTargetType(Domain.class);
      }});
    }});
    return reader;
  }

  @Bean
  public FlatFileItemWriter<Domain> writer() {
    FlatFileItemWriter<Domain> writer = new FlatFileItemWriter<>();
    writer.setResource(resource);
    writer.setLineAggregator(new DelimitedLineAggregator<Domain>() {{
      setDelimiter(",");
      setFieldExtractor(new BeanWrapperFieldExtractor<Domain>() {{
        setNames(new String[]{"id", "domain"});
      }});
    }});
    return writer;
  }
}
