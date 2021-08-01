FROM ruby:2.7.4-buster
COPY Gemfile .
RUN gem install bundler && bundle install && rm Gemfile

