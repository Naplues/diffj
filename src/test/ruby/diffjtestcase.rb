#!/usr/bin/jruby -w
# -*- ruby -*-

require 'test/unit'
require 'java'
require 'rubygems'
require 'riel'
require 'diffj'

include Java

class DiffJTestCase < Test::Unit::TestCase
  include Loggable
  
  TESTBED_DIR = '/proj/org/incava/diffj/src/test/resources'
  
  def run_diffj_test dirname
    fnames = %w{ d0 d1 }.collect { |subdir| TESTBED_DIR + '/' + dirname + '/' + subdir }
    brief = false
    context = true
    highlight = true
    recurse = true
    fromname = nil
    fromver = "1.5"
    toname = nil
    tover = "1.5"
    
    diffj = DiffJ::CLI.new brief, context, highlight, recurse, fromname, fromver, toname, tover
    diffj.process_names fnames
    assert_not_nil diffj
  end

  def get_diffj 
    brief = false
    context = true
    highlight = true
    recurse = true
    fromname = nil
    fromver = "1.5"
    toname = nil
    tover = "1.5"
    
    DiffJ::CLI.new brief, context, highlight, recurse, fromname, fromver, toname, tover
  end

  def get_from_and_to_filenames dirname, basename
    fnames = %w{ d0 d1 }.collect { |subdir| TESTBED_DIR + '/' + dirname + '/' + subdir + '/' + basename + '.java' }

    fromname, toname = *fnames
    info "fromname: #{fromname}"
    info "toname: #{toname}"

    [ fromname, toname ]
  end

  def assert_differences_match expected_diffs, actual_diffs
    info "expected_diffs: #{expected_diffs.class}"
    info "actual_diffs: #{actual_diffs.class}"

    if expected_diffs.length != actual_diffs.length
      info "mismatched number of diffs".red
      maxlen = [ expected_diffs.length, actual_diffs.length ].max
      (0 ... maxlen).each do |fdidx|
        info "expected_diffs[#{fdidx}]: #{expected_diffs[fdidx]}; #{expected_diffs[fdidx].class}"
      end
      assert_equal expected_diffs.length, actual_diffs.length, "mismatched number of diffs"
    else
      actual_diffs.each_with_index do |actdiff, fdidx|
        expdiff = expected_diffs[fdidx]
        assert_diffs_equal expdiff, actdiff, "diff[#{fdidx}]"
      end      
    end    
  end

  def run_fdiff_test expected_fdiffs, dirname, basename
    diffj = get_diffj
    report = diffj.report
    info "report: #{report}"
    info "report.differences: #{report.differences}"

    fnames = %w{ d0 d1 }.collect { |subdir| TESTBED_DIR + '/' + dirname + '/' + subdir + '/' + basename + '.java' }

    fromname, toname = *fnames
    info "fromname: #{fromname}"
    info "toname: #{toname}"

    fromname, toname = get_from_and_to_filenames dirname, basename

    fromfile = diffj.create_from_element fromname
    tofile = diffj.create_to_element toname

    info "report: #{report}"
    info "report.differences: #{report.differences}"

    fromfile.compare report, tofile

    info "report: #{report}"
    info "report.differences: #{report.differences}"

    actual_fdiffs = report.differences

    info "expected_fdiffs: #{expected_fdiffs.class}"
    info "actual_fdiffs: #{actual_fdiffs.class}"

    assert_differences_match expected_fdiffs, actual_fdiffs
  end

  def assert_equal exp, act, msg
    begin
      super
    rescue => e
      info "msg: #{msg}".red
      info "exp: #{exp}".red
      info "exp: #{exp.class}".red
      info "act: #{act}".red
      info "act: #{act.class}".red      
      raise e
    end
  end

  def assert_diffs_equal exp, act, msg = ""
    info "exp.type: #{exp.type}"
    info "act.type: #{act.type}"
    assert_equal exp.type, act.type, msg + ".type"
    assert_equal exp.message, act.message, msg + ".message"

    assert_equal exp.first_location, act.first_location, msg + ".first_location"

    info "exp.second_location: #{exp.second_location}"
    info "act.second_location: #{act.second_location}"

    assert_equal exp.second_location, act.second_location, msg + ".second_location"
  end

  def loc x, y
    org.incava.ijdk.text.Location.new x, y
  end

  def loctext loc, text
    loc loc.line, loc.column + text.length - 1
  end

  def test_nothing
  end

  def format msg, *values
    java.text.MessageFormat.format msg, *values
  end

  def make_fdiff type, msgvals, from_start, from_end, to_start, to_end
    msg = msgvals.kind_of?(Array) && msgvals.length > 1 ? format(msgvals[0], *(msgvals[1 .. -1])) : msgvals
    type.new(msg, from_start, from_end, to_start, to_end)
  end

  def make_fdiff_add msgvals, from_start, from_end, to_start, to_end
    make_fdiff org.incava.analysis.FileDiffAdd, msgvals, from_start, from_end, to_start, to_end
  end

  def make_fdiff_delete msgvals, from_start, from_end, to_start, to_end
    make_fdiff org.incava.analysis.FileDiffDelete, msgvals, from_start, from_end, to_start, to_end
  end

  def make_fdiff_change msgvals, from_start, from_end, to_start, to_end
    make_fdiff org.incava.analysis.FileDiffChange, msgvals, from_start, from_end, to_start, to_end
  end

  def subdir
    raise "subdir: must be implemented by subclasses"
  end  

  def added_msg_fmt
    raise "added_msg_fmt: must be implemented by subclasses"
  end  

  def removed_msg_fmt
    raise "removed_msg_fmt: must be implemented by subclasses"
  end

  def changed_msg_fmt
    raise "changed_msg_fmt: must be implemented by subclasses"
  end  
  
  # a filediff with type "change" (so from and to contexts are displayed), but
  # with a message of the form: "X added".
  def added_change what, from_start, from_end, to_start, to_end = nil
    make_fdiff_change format(added_msg_fmt, what), from_start, from_end, to_start, to_end || loctext(to_start, what)
  end

  def added_add what, from_start, from_end, to_start, to_end = nil
    make_fdiff_add format(added_msg_fmt, what), from_start, from_end, to_start, to_end || loctext(to_start, what)
  end

  def added what, from_start, from_end, to_start, to_end = nil
    stack "warning: use added_change".red
    make_fdiff_change format(added_msg_fmt, what), from_start, from_end, to_start, to_end || loctext(to_start, what)
  end

  def removed what, from_start, to_start, to_end
    make_fdiff_change format(removed_msg_fmt, what), from_start, loctext(from_start, what), to_start, to_end
  end

  def removed_change what, from_start, to_start, to_end
    make_fdiff_change format(removed_msg_fmt, what), from_start, loctext(from_start, what), to_start, to_end
  end

  def removed_delete what, from_start, to_start, to_end
    make_fdiff_delete format(removed_msg_fmt, what), from_start, loctext(from_start, what), to_start, to_end
  end

  def changed from, to, from_start, to_start
    make_fdiff_change format(changed_msg_fmt, from, to), from_start, loctext(from_start, from), to_start, loctext(to_start, to)
  end
end